package pt.controleobras.app.feature.receiptflow.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.controleobras.app.core.device.DeviceInfo
import pt.controleobras.app.core.drive.DriveUploader
import pt.controleobras.app.core.export.CapturaCsvExporter
import pt.controleobras.app.core.extractor.PositionAwareReceiptExtractor
// import removido: LlmExtractionResult já não é usado (código morto eliminado)
import pt.controleobras.app.core.llm.LlmExtractor
// import removido: LlmItemResult já não é usado (código morto eliminado)
import pt.controleobras.app.core.location.LocationProvider
import pt.controleobras.app.core.model.CaptureMetadata
import pt.controleobras.app.core.model.ItemTalaoDraft
import pt.controleobras.app.core.model.TalaoDraft
import pt.controleobras.app.core.database.remote.FrefRepository
import pt.controleobras.app.core.model.CentroCusto
import pt.controleobras.app.core.model.WorkerFormData
import pt.controleobras.app.core.model.paraDominio
import pt.controleobras.app.core.ocr.TextRecognizer
import pt.controleobras.app.core.parser.ReceiptParser
import pt.controleobras.app.core.qr.AtQrCodeParser
import pt.controleobras.app.core.common.AppPreferences
import pt.controleobras.app.core.qr.AtQrData
import pt.controleobras.app.core.qr.QrCodeReader
import pt.controleobras.app.core.validation.InvoiceFieldValidator
import pt.controleobras.app.data.repository.TalaoRepository
import java.io.File
import java.time.LocalDate
// imports removidos: LocalTime e DateTimeFormatter já não são usados após remoção do código morto
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
    /** ML Kit Text Recognition — converte a imagem da fatura em texto estruturado. */
    private val textRecognizer: TextRecognizer,
    /** ML Kit Barcode Scanner — lê QR codes da imagem (ex: QR code AT). */
    private val qrCodeReader: QrCodeReader,
    /** Parser heurístico (fallback) — extrai campos usando regex sobre texto plano. */
    private val receiptParser: ReceiptParser,
    /** Interpreta o conteúdo específico do QR code AT (Portaria 195/2020). */
    private val atQrCodeParser: AtQrCodeParser,
    /** Extrator baseado em modelo de IA local (Gemma) — atualmente não é o caminho principal. */
    private val llmExtractor: LlmExtractor,
    /** Extrator posicional (primário) — usa a posição visual dos elementos na fatura. */
    private val positionExtractor: PositionAwareReceiptExtractor,
    /** Valida cada campo extraído (NIF, datas, valores) e atribui níveis de confiança. */
    private val invoiceValidator: InvoiceFieldValidator,
    /** Obtém coordenadas GPS do dispositivo (para geolocalizar a fatura). */
    private val locationProvider: LocationProvider,
    /** Obtém informação do dispositivo (número de série, geração de IDs únicos). */
    private val deviceInfo: DeviceInfo,
    /** Gera o ficheiro CSV de registo da captura (formato exigido pela empresa). */
    private val capturaCsvExporter: CapturaCsvExporter,
    /** Faz upload de ficheiros para a pasta Google Drive configurada. */
    private val driveUploader: DriveUploader,
    /** Repositório que gere a persistência de talões na base de dados Room local. */
    private val talaoRepository: TalaoRepository,
    /** Preferências partilhadas da app (último feedback, download ID, etc.). */
    private val appPreferences: AppPreferences,
    /** Repositório de centros de custo sincronizados com MariaDB. */
    private val frefRepository: FrefRepository
) : ViewModel() {

    /**
     * Lista de centros de custo (obras) da cache local (Room).
     * Preenchida após sincronização com o servidor MariaDB.
     * WhileSubscribed(5s) mantém o Flow ativo 5 segundos após o último observador
     * para evitar re-consultas desnecessárias à BD quando o utilizador roda o ecrã.
     */
    val centroCustos: StateFlow<List<CentroCusto>> = frefRepository.listarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Estado completo do fluxo de captura — observado pelos 3 ecrãs (Form, Câmara, Revisão). */
    private val _uiState = MutableStateFlow(ReceiptFlowUiState())
    val uiState: StateFlow<ReceiptFlowUiState> = _uiState.asStateFlow()

    // -----------------------------------------------------------------------
    // Formulário do funcionário
    // -----------------------------------------------------------------------

    /**
     * Guarda os dados do formulário do funcionário no estado partilhado.
     * Chamado pelo WorkerFormScreen quando o utilizador carrega em "Continuar".
     * Estes dados serão gravados junto com o talão na BD.
     */
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
                val serial = deviceInfo.getSerialNumber(context)
                val idReg  = deviceInfo.gerarIdReg()
                val metadata = CaptureMetadata(
                    serialDispositivo = serial,
                    idReg             = idReg,
                    gps               = gps,
                    qrCodeRaw         = qrContent
                )

                // 3. Mover imagem para pasta definitiva {SERIAL}_{IDREG}.jpg
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
                    workerData = _uiState.value.workerFormData ?: WorkerFormData("", null, ""),
                    atQrData   = atQrData,
                    draft      = draft
                )

                // 9. Atualizar estado — ecrã de revisão já está aberto, apenas preenche campos
                //    Se não houver QR, ativa automaticamente o diálogo de dados manuais.
                //    A imagem final é guardada no estado para o upload Drive posterior.
                _uiState.update {
                    it.copy(
                        isProcessing            = false,
                        statusProcessamento     = "",
                        draft                   = draft,
                        validacoes              = validacoes,
                        captureMetadata         = metadata,
                        qrDetectado             = atQrData != null,
                        driveStatus             = DriveStatus.IDLE,
                        mostrarDialogoNifManual = atQrData == null,
                        imagemFinalPath         = imagemFinal.absolutePath
                    )
                }

                // 10. Upload Drive — apenas quando há QR (CSV já está completo).
                //     Sem QR, o upload é feito em confirmarEGuardar() após o utilizador
                //     preencher NIF e valor, garantindo que MNIF e MVALOR chegam ao Drive.
                if (atQrData != null) {
                    uploadDrive(context, imagemFinal, csvFile)
                }

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
    // Guardar talão
    // -----------------------------------------------------------------------
    // NOTA: As funções llmResultToDraft(), parsarDataLlm(), parsarHoraLlm()
    // e LlmItemResult.toItemTalaoDraft() foram removidas por serem código morto.
    // Nenhum caminho do código as chamava — o processamento LLM usa outro fluxo.
    // Se no futuro for necessário converter LlmExtractionResult → TalaoDraft,
    // devem ser recriadas com testes adequados.

    fun atualizarDraft(transform: (TalaoDraft) -> TalaoDraft) {
        _uiState.update { estado ->
            estado.draft?.let { estado.copy(draft = transform(it)) } ?: estado
        }
    }

    fun confirmarEGuardar(context: Context? = null) {
        val estado = _uiState.value
        val draft  = estado.draft ?: return

        viewModelScope.launch {
            // Sem QR: regenerar o CSV final com MNIF e MVALOR preenchidos,
            // depois fazer upload Drive. É aqui que os dados manuais chegam ao ficheiro.
            if (context != null && !estado.qrDetectado) {
                val metadata   = estado.captureMetadata
                val workerData = estado.workerFormData
                if (metadata != null && workerData != null) {
                    val csvFinal = runCatching {
                        val destDir = java.io.File(context.filesDir, "receipts").also { it.mkdirs() }
                        capturaCsvExporter.exportar(
                            destDir     = destDir,
                            metadata    = metadata,
                            workerData  = workerData,
                            atQrData    = null,
                            draft       = draft,
                            nifManual   = estado.nifManual,
                            valorManual = estado.valorManual
                        )
                    }.getOrNull()

                    // Upload Drive com o CSV final (que já tem MNIF e MVALOR)
                    if (csvFinal != null && estado.imagemFinalPath != null) {
                        val imagem = java.io.File(estado.imagemFinalPath)
                        uploadDrive(context, imagem, csvFinal)
                    }
                }
            }

            val workerFormData = estado.workerFormData
            runCatching { draft.paraDominio() }
                .onSuccess { talao ->
                    val cc = workerFormData?.centroCusto
                    val talaoComWorker = talao.copy(
                        funcn  = workerFormData?.funcn  ?: "",
                        fref   = cc?.fref               ?: "",
                        nmfref = cc?.nmfref             ?: "",
                        agnome = cc?.agnome             ?: ""
                    )
                    val id = talaoRepository.guardar(talaoComWorker)
                    // Gravar feedback para mostrar no HomeScreen ao regressar
                    val empresa = talao.empresa.ifBlank { "Fatura" }
                    val total   = talao.total?.toString()?.let { "${it} €" } ?: ""
                    val ts      = System.currentTimeMillis()
                    appPreferences.ultimaFaturaFeedback = "$empresa|$total|$ts"
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

    // -----------------------------------------------------------------------
    // Dados manuais (NIF + Valor) quando não há QR code AT
    // -----------------------------------------------------------------------

    /**
     * Chamado quando o utilizador confirma NIF e valor no diálogo de introdução manual.
     * Guarda os dados no estado (serão exportados como MNIF e MVALOR no CSV)
     * e dispara imediatamente [confirmarEGuardar] para guardar o talão.
     */
    fun definirDadosManuaisEGuardar(context: Context, nif: String, valor: String) {
        _uiState.update {
            it.copy(
                nifManual               = nif.trim().ifBlank { null },
                valorManual             = valor.trim().ifBlank { null },
                mostrarDialogoNifManual = false,
                dadosManuaisConfirmados = true
            )
        }
        confirmarEGuardar(context)
    }

    /**
     * Reabre o diálogo de dado
     * Reabre o diálogo de dados manuais (NIF + Valor).
     */
    fun reabrirDialogoNifManual() {
        _uiState.update { it.copy(mostrarDialogoNifManual = true) }
    }

    // Mantido para compatibilidade — não deve ser usado na UI (diálogo é obrigatório)
    @Deprecated("Usar reabrirDialogoNifManual. Sem QR o preenchimento é obrigatório.")
    fun fecharDialogoNifManual() {
        _uiState.update { it.copy(mostrarDialogoNifManual = false) }
    }
}
