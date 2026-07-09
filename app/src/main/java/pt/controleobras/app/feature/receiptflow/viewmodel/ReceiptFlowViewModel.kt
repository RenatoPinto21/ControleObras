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
import pt.controleobras.app.core.location.LocationProvider
import pt.controleobras.app.core.model.CaptureMetadata
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
import javax.inject.Inject

/**
 * Orquestra o fluxo completo:
 *
 *   WorkerForm → Câmara → [foto]
 *     → OCR + QR (paralelo) + GPS + MAC + IDREG
 *     → renomeia imagem para {MAC}_{IDREG}.jpg
 *     → gera CSV {MAC}_{IDREG}.csv
 *     → envia ambos para Google Drive (background)
 *     → ecrã de revisão com dados extraídos
 *
 * Partilhado entre os ecrãs de formulário, captura e revisão via grafo aninhado.
 */
@HiltViewModel
class ReceiptFlowViewModel @Inject constructor(
    private val textRecognizer: TextRecognizer,
    private val qrCodeReader: QrCodeReader,
    private val receiptParser: ReceiptParser,
    private val atQrCodeParser: AtQrCodeParser,
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
        _uiState.update { it.copy(isProcessing = true, errorMessage = null, qrDetectado = false) }

        viewModelScope.launch {
            runCatching {
                // 1. Lançar OCR, QR e GPS em paralelo
                val textoDeferred = async { textRecognizer.recognizeText(context, imagemUri) }
                val qrDeferred = async {
                    runCatching { qrCodeReader.readQrCode(context, imagemUri) }.getOrNull()
                }
                val gpsDeferred = async { locationProvider.getCurrentLocation(context) }

                val texto = textoDeferred.await()
                val qrContent = qrDeferred.await()
                val gps = gpsDeferred.await()

                // 2. Gerar identificadores do registo
                val mac = deviceInfo.getMacAddress(context)
                val idReg = deviceInfo.gerarIdReg()
                val metadata = CaptureMetadata(
                    macAddress = mac,
                    idReg = idReg,
                    gps = gps,
                    qrCodeRaw = qrContent
                )

                // 3. Renomear a imagem original para {MAC}_{IDREG}.jpg
                val destDir = File(context.filesDir, "receipts").also { it.mkdirs() }
                val imagemFinal = File(destDir, "${metadata.fileBaseName}.jpg")
                File(imagemPath).copyTo(imagemFinal, overwrite = true)

                // 4. OCR + QR → draft
                val atQrData = qrContent?.let { atQrCodeParser.parse(it) }
                val draftOcr = receiptParser.parse(texto, imagemFinal.absolutePath)
                val draft = mergeDraft(draftOcr, atQrData)

                // 5. Gerar CSV de registo
                val csvFile = capturaCsvExporter.exportar(
                    destDir = destDir,
                    metadata = metadata,
                    workerData = _uiState.value.workerFormData
                        ?: WorkerFormData("", "", ""),
                    atQrData = atQrData,
                    fornecedor = draft.empresa
                )

                // 6. Atualizar estado (navega para revisão)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        draft = draft,
                        captureMetadata = metadata,
                        qrDetectado = atQrData != null,
                        driveStatus = DriveStatus.IDLE
                    )
                }

                // 7. Upload Drive em background (não bloqueia a UI)
                uploadDrive(context, imagemFinal, csvFile)

            }.onFailure { erro ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
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
    // Merge OCR + QR
    // -----------------------------------------------------------------------

    private fun mergeDraft(draftOcr: TalaoDraft, qr: AtQrData?): TalaoDraft {
        if (qr == null) return draftOcr
        return draftOcr.copy(
            nif = qr.nif.ifBlank { draftOcr.nif },
            data = qr.data ?: draftOcr.data,
            numeroFatura = qr.numeroFatura?.ifBlank { null } ?: draftOcr.numeroFatura,
            iva = qr.totalIva?.ifBlank { null } ?: draftOcr.iva,
            total = qr.totalComIva?.ifBlank { null } ?: draftOcr.total
        )
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
