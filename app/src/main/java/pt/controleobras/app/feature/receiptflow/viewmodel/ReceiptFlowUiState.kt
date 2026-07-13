package pt.controleobras.app.feature.receiptflow.viewmodel

import pt.controleobras.app.core.model.CaptureMetadata
import pt.controleobras.app.core.model.TalaoDraft
import pt.controleobras.app.core.model.WorkerFormData
import pt.controleobras.app.core.validation.FieldValidation

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
    val driveStatus: DriveStatus = DriveStatus.IDLE,

    /**
     * Path da imagem capturada — definido IMEDIATAMENTE quando a foto é tirada,
     * antes do OCR. Permite navegar para o ecrã de revisão de imediato
     * e mostrar a imagem enquanto o processamento corre em background.
     */
    val imagemCapturadaPath: String? = null,

    /**
     * Resultado da validação de cada campo do draft.
     * Chave = nome do campo (ex: "nif", "total", "data").
     * Valor = [FieldValidation] com estado VALID / SUSPECT / MISSING.
     * Vazio enquanto o processamento não terminar.
     */
    val validacoes: Map<String, FieldValidation> = emptyMap()
)

enum class DriveStatus { IDLE, A_ENVIAR, ENVIADO, ERRO }
