package pt.controleobras.app.feature.receiptflow.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.controleobras.app.core.device.DeviceInfo
import pt.controleobras.app.core.drive.DriveUploader
import pt.controleobras.app.core.export.CapturaCsvExporter
import pt.controleobras.app.core.llm.LlmExtractionResult
import pt.controleobras.app.core.llm.LlmExtractor
import pt.controleobras.app.core.llm.LlmItemResult
import pt.controleobras.app.core.location.LocationProvider
import pt.controleobras.app.core.model.CaptureMetadata
import pt.controleobras.app.core.model.ItemTalaoDraft
import pt.controleobras.app.core.model.TalaoDraft
import pt.controleobras.app.core.model.WorkerFormData
import pt.controleobras.app.core.model.paraDominio
import pt.controleobras.app.core.ocr.TextRecognizer
import pt.controleobras.app.core.parser.ReceiptParser
import pt.controleobras.app.core.qr.AtQrCodeParser
import pt.controleobras.app.core.qr.AtQrData
import pt.controleobras.app.core.qr.QrCodeReader
import pt.controleobras.app.data.repository.TalaoRepository
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Orquestra o fluxo completo:
 *
 *   WorkerForm → Câmara → [foto]
 *     → OCR + QR (paralelo) + GPS + MAC + IDREG
 *     → LLM (Gemma 3 1B, se instalado) ou HeuristicParser (fallback)
 *     → renomeia imagem para {MAC}_{IDREG}.jpg
 *     → gera CSV {MAC}_{IDREG}.csv
 *     → envia ambos para Google Drive (background)
 *     → ecrã de revisão com dados extraídos
 *
 * Hierarquia de extração (da mais fiável para a menos):
 *   1. QR code AT  (Portaria 195/2020 — dados inseridos pelo emitente)
 *   2. LLM Gemma   (compreensão semântica do texto OCR)
 *   3. Heurístico  (regex — sempre disponível, sem dependências)
 *
 * Partilhado entre os ecrãs de formulário, captura e revisão via grafo aninhado.
 */
@HiltViewModel
class ReceiptFlowViewModel @Inject constructor(
    private val textRecognizer: TextRecognizer,
    private val qrCodeReader: QrCodeReader,
    private val receiptParser: ReceiptParser,
    private val atQrCodeParser: AtQrCodeParser,
    private val llmExtractor: LlmExtractor,
    private val locationProvider: LocationProvider,
    private val deviceInfo: DeviceInfo,
    private val capturaCsvExporter: CapturaCsvExporter,
    private val driveUploader: DriveUploader,
    private val talaoRepository: TalaoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptFlowUiState())
    val uiState: StateFlow<ReceiptFlowUiState> = _uiState.asStateFlow()

    // -----------------------------------------------------------------------
    // Formulário do funcionário
    // -----------------------------------------------------------------------

    fun definirFormulario(data: WorkerFormData) {
        _uiState.update { it.copy(workerFormData = data) }
    }

    // -----------------------------------------------------------------------
    // Processamento da imagem
    // -----------------------------------------------------------------------

    fun processarImagem(context: Context, imagemUri: Uri, imagemPath: String) {
        _uiState.update {
            it.copy(
                isProcessing = true,
                statusProcessamento = "A ler imagem (OCR)...",
                errorMessage = null,
                qrDetectado = false
            )
        }

        viewModelScope.launch {
            runCatching {
                // 1. OCR, QR e GPS em paralelo — maximiza aproveitamento do tempo
                val textoDeferred = async { textRecognizer.recognizeText(context, imagemUri) }
                val qrDeferred = async {
                    runCatching { qrCodeReader.readQrCode(context, imagemUri) }.getOrNull()
                }
                val gpsDeferred = async { locationProvider.getCurrentLocation(context) }

                val texto = textoDeferred.await()
                val qrContent = qrDeferred.await()
                val gps = gpsDeferred.await()

                // 2. Identificadores do registo
                val mac = deviceInfo.getMacAddress(context)
                val idReg = deviceInfo.gerarIdReg()
                val metadata = CaptureMetadata(
                    macAddress = mac,
                    idReg = idReg,
                    gps = gps,
                    qrCodeRaw = qrContent
                )

                // 3. Renomear imagem para {MAC}_{IDREG}.jpg
                val destDir = File(context.filesDir, "receipts").also { it.mkdirs() }
                val imagemFinal = File(destDir, "${metadata.fileBaseName}.jpg")
                File(imagemPath).copyTo(imagemFinal, overwrite = true)

                // 4. Interpretação AT QR code (mais fiável — dados do emitente)
                val atQrData = qrContent?.let { atQrCodeParser.parse(it) }

                // 5. Extração semântica: LLM (se instalado) ou heurístico (fallback)
                val draftBase = if (llmExtractor.isModelReady()) {
                    _uiState.update {
                        it.copy(statusProcessamento = "A analisar com IA (Gemma)...")
                    }
                    val llmResult = runCatching { llmExtractor.extract(texto) }.getOrNull()
                    llmResult?.let { llmResultToDraft(it, imagemFinal.absolutePath, texto) }
                        ?: receiptParser.parse(texto, imagemFinal.absolutePath)  // fallback
                } else {
                    receiptParser.parse(texto, imagemFinal.absolutePath)
                }

                // 6. Merge com QR (QR tem prioridade para os campos que fornece)
                val draft = mergeDraft(draftBase, atQrData)

                // 7. Gerar CSV de registo
                val csvFile = capturaCsvExporter.exportar(
                    destDir = destDir,
                    metadata = metadata,
                    workerData = _uiState.value.workerFormData
                        ?: WorkerFormData("", "", ""),
                    atQrData = atQrData,
                    draft = draft
                )

                // 8. Atualizar estado (navega para revisão)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusProcessamento = "",
                        draft = draft,
                        captureMetadata = metadata,
                        qrDetectado = atQrData != null,
                        driveStatus = DriveStatus.IDLE
                    )
                }

                // 9. Upload Drive em background (não bloqueia a UI)
                uploadDrive(context, imagemFinal, csvFile)

            }.onFailure { erro ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusProcessamento = "",
                        errorMessage = erro.message ?: "Falha ao processar a imagem."
                    )
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Upload Google Drive (background)
    // -----------------------------------------------------------------------

    private fun uploadDrive(context: Context, imagem: File, csv: File) {
        if (!driveUploader.isConfigurado(context)) return
        viewModelScope.launch {
            _uiState.update { it.copy(driveStatus = DriveStatus.A_ENVIAR) }
            val imagemOk = driveUploader.upload(context, imagem, "image/jpeg")
            val csvOk = driveUploader.upload(context, csv, "text/csv")
            _uiState.update {
                it.copy(driveStatus = if (imagemOk && csvOk) DriveStatus.ENVIADO else DriveStatus.ERRO)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Merge: QR code AT tem prioridade sobre LLM/heurístico
    // -----------------------------------------------------------------------

    private fun mergeDraft(base: TalaoDraft, qr: AtQrData?): TalaoDraft {
        if (qr == null) return base
        return base.copy(
            nif          = qr.nif.ifBlank { base.nif },
            data         = qr.data ?: base.data,
            numeroFatura = qr.numeroFatura?.ifBlank { null } ?: base.numeroFatura,
            iva          = qr.totalIva?.ifBlank { null } ?: base.iva,
            total        = qr.totalComIva?.ifBlank { null } ?: base.total
        )
    }

    // -----------------------------------------------------------------------
    // Conversão LlmExtractionResult → TalaoDraft
    // -----------------------------------------------------------------------

    private fun llmResultToDraft(
        result: LlmExtractionResult,
        imagemPath: String,
        textoOcr: String
    ): TalaoDraft = TalaoDraft(
        empresa         = result.fornecedor.orEmpty(),
        nif             = result.nifFornecedor.orEmpty(),
        nifCliente      = result.nifCliente.orEmpty(),
        morada          = result.morada.orEmpty(),
        serie           = result.serie.orEmpty(),
        numeroFatura    = result.numeroFatura.orEmpty(),
        data            = parsarDataLlm(result.dataEmissao),
        dataVencimento  = parsarDataLlm(result.dataVencimento),
        hora            = parsarHoraLlm(result.hora),
        metodoPagamento = result.metodoPagamento.orEmpty(),
        total           = result.total?.replace(',', '.').orEmpty(),
        iva             = result.ivaTotal?.replace(',', '.').orEmpty(),
        itens           = result.linhas.map { it.toItemTalaoDraft() },
        observacoes     = result.observacoes.orEmpty(),
        imagemPath      = imagemPath,
        textoReconhecido = textoOcr
    )

    private fun LlmItemResult.toItemTalaoDraft() = ItemTalaoDraft(
        descricao     = descricao.orEmpty(),
        quantidade    = quantidade.orEmpty(),
        precoUnitario = precoUnitario?.replace(',', '.').orEmpty(),
        desconto      = desconto?.replace(',', '.').orEmpty(),
        taxaIva       = taxaIva.orEmpty(),
        total         = totalLinha?.replace(',', '.').orEmpty()
    )

    private fun parsarDataLlm(valor: String?): LocalDate? {
        valor ?: return null
        val formatos = listOf(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy")
        )
        for (fmt in formatos) {
            runCatching { LocalDate.parse(valor.trim(), fmt) }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun parsarHoraLlm(valor: String?): LocalTime? {
        valor ?: return null
        return runCatching {
            LocalTime.parse(valor.trim(), DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrNull() ?: runCatching {
            LocalTime.parse(valor.trim(), DateTimeFormatter.ofPattern("H:mm"))
        }.getOrNull()
    }

    // -----------------------------------------------------------------------
    // Guardar talão
    // -----------------------------------------------------------------------

    fun atualizarDraft(transform: (TalaoDraft) -> TalaoDraft) {
        _uiState.update { estado ->
            estado.draft?.let { estado.copy(draft = transform(it)) } ?: estado
        }
    }

    fun confirmarEGuardar() {
        val draft = _uiState.value.draft ?: return
        viewModelScope.launch {
            runCatching { draft.paraDominio() }
                .onSuccess { talao ->
                    val id = talaoRepository.guardar(talao)
                    _uiState.update { it.copy(savedTalaoId = id) }
                }
                .onFailure { erro ->
                    _uiState.update { it.copy(errorMessage = erro.message ?: "Dados inválidos.") }
                }
        }
    }

    fun limparErro() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
