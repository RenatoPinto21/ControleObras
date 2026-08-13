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

    /** Metadados técnicos da captura (SERIAL, IDREG, GPS, QR raw). */
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
    val validacoes: Map<String, FieldValidation> = emptyMap(),

    /**
     * NIF introduzido manualmente pelo utilizador quando o QR code AT não foi detetado.
     * Null se o QR foi lido com sucesso ou se o utilizador ainda não introduziu.
     * Exportado para a coluna MNIF do CSV apenas quando presente.
     */
    val nifManual: String? = null,

    /**
     * Valor total introduzido manualmente pelo utilizador quando o QR code AT não foi detetado.
     * Null se o QR foi lido com sucesso ou se o utilizador ainda não introduziu.
     * Exportado para a coluna MVALOR do CSV apenas quando presente.
     */
    val valorManual: String? = null,

    /**
     * Controla a visibilidade do diálogo de introdução manual de NIF e valor.
     * Ativado automaticamente quando o processamento termina sem QR code detetado.
     */
    val mostrarDialogoNifManual: Boolean = false,

    /**
     * True quando o utilizador confirmou o diálogo de dados manuais (NIF + valor).
     * Sem QR, o botão "Guardar" fica bloqueado até este campo ser true.
     */
    val dadosManuaisConfirmados: Boolean = false,

    /**
     * Path do ficheiro de imagem final (após correcção de orientação).
     * Guardado para que o upload Drive possa ser feito em confirmarEGuardar()
     * nos casos sem QR, depois de MNIF/MVALOR estarem preenchidos.
     */
    val imagemFinalPath: String? = null
)

enum class DriveStatus { IDLE, A_ENVIAR, ENVIADO, ERRO }
