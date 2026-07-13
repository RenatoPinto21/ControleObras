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
import pt.controleobras.app.core.extractor.PositionAwareReceiptExtractor
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
import pt.controleobras.app.core.validation.InvoiceFieldValidator
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
 *     → OCR estruturado + QR + GPS (em paralelo)
 *     → PositionAwareReceiptExtractor (primário — posição visual dos elementos)
 *     → HeuristicParser (fallback — apenas texto)
 *     → merge com QR code AT (QR tem prioridade sobre OCR)
 *     → InvoiceFieldValidator (valida NIF, datas, valores, cruzamentos)
 *     → renomeia imagem + gera CSV
 *     → Google Drive (background)
 *     → ecrã de revisão com 3 estados visuais (verde/amarelo/cinza)
 *
 * Hierarquia de extração (da mais fiável para a menos):
 *   1. QR code AT  (Portaria 195/2020 — dados emitidos e assinados pelo vendedor)
 *   2. Posicional  (usa layout visual da fatura — zonas header/corpo/rodapé)
 *   3. Heurístico  (regex sobre texto concatenado — fallback sem layout)
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
    private val positionExtractor: PositionAwareReceiptExtractor,
    private val invoiceValidator: InvoiceFieldValidator,
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
        // Define o path da imagem IMEDIATAMENTE — antes do OCR/LLM.
        // Isto permite que o ecrã de revisão abra já com a imagem visível
        // enquanto o processamento (OCR + IA) corre em background.
        _uiState.update {
            it.copy(
                isProcessing = true,
                statusProcessamento = "A ler imagem (OCR)...",
                errorMessage = null,
                qrDetectado = false,
                draft = null,
                imagemCapturadaPath = imagemPath
            )
        }

        viewModelScope.launch {
            runCatching {
                // 1. OCR estruturado + QR + GPS em paralelo
                val ocrDeferred = async { textRecognizer.recognizeStructured(context, imagemUri) }
                val qrDeferred  = async {
                    runCatching { qrCodeReader.readQrCode(context, imagemUri) }.getOrNull()
                }
                val gpsDeferred = async { locationProvider.getCurrentLocation(context) }

                val ocrResult  = ocrDeferred.await()
                val qrContent  = qrDeferred.await()
                val gps        = gpsDeferred.await()

                // 2. Identificadores do registo
                val mac    = deviceInfo.getMacAddress(context)
                val idReg  = deviceInfo.gerarIdReg()
                val metadata = CaptureMetadata(
                    macAddress = mac,
                    idReg      = idReg,
                    gps        = gps,
                    qrCodeRaw  = qrContent
                )

                // 3. Mover imagem para pasta definitiva {MAC}_{IDREG}.jpg
                val destDir    = File(context.filesDir, "receipts").also { it.mkdirs() }
                val imagemFinal = File(destDir, "${metadata.fileBaseName}.jpg")
                File(imagemPath).copyTo(imagemFinal, overwrite = true)

                // 4. QR code AT — mais fiável (dados do emitente, assinados)
                val atQrData = qrContent?.let { atQrCodeParser.parse(it) }

                // 5. Extração posicional (primária) — usa layout visual da fatura
                _uiState.update { it.copy(statusProcessamento = "A interpretar estrutura da fatura...") }
                val draftBase = runCatching {
                    positionExtractor.extract(ocrResult, imagemFinal.absolutePath)
                }.getOrElse {
                    // Fallback: parser heurístico sobre texto concatenado
                    receiptParser.parse(ocrResult.fullText, imagemFinal.absolutePath)
                }

                // 6. Merge com QR (QR sobrepõe campos que fornece)
                val draft = mergeDraft(draftBase, atQrData)

                // 7. Validação de todos os campos (temQr determina confiança dos valores OCR)
                val validacoes = invoiceValidator.validate(draft, temQr = atQrData != null)

                // 8. Gerar CSV de registo
                val csvFile = capturaCsvExporter.exportar(
                    destDir    = destDir,
                    metadata   = metadata,
                    workerData = _uiState.value.workerFormData ?: WorkerFormData("", "", ""),
                    atQrData   = atQrData,
                    draft      = draft
                )

                // 9. Atualizar estado — ecrã de revisão já está aberto, apenas preenche campos
                _uiState.update {
                    it.copy(
                        isProcessing        = false,
                        statusProcessamento = "",
                        draft               = draft,
                        validacoes          = validacoes,
                        captureMetadata     = metadata,
                        qrDetectado         = atQrData != null,
                        driveStatus         = DriveStatus.IDLE
                    )
                }

                // 10. Upload Drive em background (não bloqueia a UI)
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
    // Processar QR code lido pela QrScanScreen (câmara em tempo real)
    // -----------------------------------------------------------------------

    /**
     * Chamado pela [QrScanScreen] quando deteta um QR code AT.
     * Faz o merge com o draft existente e re-valida todos os campos.
     * Não precisa de contexto — o raw value já vem decodificado pelo ML Kit.
     */
    fun processarQrEscaneado(qrRawValue: String) {
        val draft = _uiState.value.draft ?: return
        val atQrData = atQrCodeParser.parse(qrRawValue) ?: run {
            _uiState.update { it.copy(errorMessage = "QR code não é um QR code AT válido") }
            return
        }
        val draftAtualizado = mergeDraft(draft, atQrData)
        val validacoes      = invoiceValidator.validate(draftAtualizado, temQr = true)
        _uiState.update {
            it.copy(
                draft        = draftAtualizado,
                validacoes   = validacoes,
                qrDetectado  = true,
                errorMessage = null
            )
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
        // NIF "999999990" significa "consumidor final" — não sobrepõe campo do cliente
        val nifClienteQr = qr.nifCliente?.takeIf { it != "999999990" }
        return base.copy(
            nif          = qr.nif.ifBlank { base.nif },
            nifCliente   = nifClienteQr ?: base.nifCliente,
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
