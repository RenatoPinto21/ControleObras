package pt.controleobras.app.feature.receiptflow.viewmodel

import pt.controleobras.app.core.model.CaptureMetadata
import pt.controleobras.app.core.model.TalaoDraft
import pt.controleobras.app.core.model.WorkerFormData

data class ReceiptFlowUiState(
    val isProcessing: Boolean = false,
    /** Mensagem de estado mostrada durante o processamento (OCR, IA, etc.). */
    val statusProcessamento: String = "",
    val draft: TalaoDraft? = null,
    val errorMessage: String? = null,
    val savedTalaoId: Long? = null,

    /** Dados preenchidos pelo funcionário no formulário inicial. */
    val workerFormData: WorkerFormData? = null,

    /** Metadados técnicos da captura (MAC, IDREG, GPS, QR raw). */
    val captureMetadata: CaptureMetadata? = null,

    /** True se o QR code AT foi detetado na imagem. */
    val qrDetectado: Boolean = false,

    /** Estado do upload para o Google Drive. */
    val driveStatus: DriveStatus = DriveStatus.IDLE
)

enum class DriveStatus { IDLE, A_ENVIAR, ENVIADO, ERRO }
