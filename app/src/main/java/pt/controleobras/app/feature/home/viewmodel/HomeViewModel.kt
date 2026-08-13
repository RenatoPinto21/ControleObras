package pt.controleobras.app.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import pt.controleobras.app.core.common.AppPreferences
import pt.controleobras.app.core.database.dao.TalaoDao
import pt.controleobras.app.core.database.remote.FrefRepository
import pt.controleobras.app.core.database.remote.ResultadoSync
import pt.controleobras.app.core.database.remote.SubFuncRepository
import pt.controleobras.app.core.device.DeviceInfo
import pt.controleobras.app.core.llm.LlmDownloadEstado
import pt.controleobras.app.core.llm.LlmDownloadProgress
import pt.controleobras.app.core.llm.LlmModelDownloader
import pt.controleobras.app.core.llm.LlmModelManager
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Tipos auxiliares do ecrã Home
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Dados do banner de feedback que aparece no topo do Home quando o utilizador
 * acabou de guardar uma fatura. Mostra o nome da empresa e o total.
 */
data class FeedbackFatura(
    val empresa: String,
    val total: String
)

/**
 * Estado da ligação ao servidor MariaDB (PHPRetailConcept/FREF).
 * Exibido como um chip colorido no cabeçalho do HomeScreen.
 *  - DESCONHECIDO: ainda não tentou ligar (estado inicial)
 *  - A_SINCRONIZAR: a tentar estabelecer ligação
 *  - LIGADO: ligação bem-sucedida, dados de centros de custo carregados
 *  - ERRO: falha na ligação (sem rede, servidor em baixo, credenciais erradas)
 */
enum class EstadoBd { DESCONHECIDO, A_SINCRONIZAR, LIGADO, ERRO }

/**
 * Resumo do dia atual para exibir no card do Home.
 * Mostra quantos talões foram registados hoje e o total gasto.
 */
data class ResumoDia(
    val totalTaloes: Int = 0,
    val totalDespesas: Double = 0.0,
    val totalPresencas: Int = 0
)

/**
 * Resumo de um período (semana/mês) para os cards do Home.
 */
data class ResumoPeriodo(
    val totalTaloesSemana: Int = 0,
    val totalDespesasSemana: Double = 0.0,
    val totalTaloesMes: Int = 0,
    val totalDespesasMes: Double = 0.0
)

/**
 * Dados resumidos de uma fatura recente para a mini-lista do Home.
 */
data class FaturaRecente(
    val id: Long,
    val empresa: String,
    val total: String,
    val data: String
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ViewModel do ecrã Home.
 *
 * Responsabilidades:
 *  1. Gerir o diálogo de boas-vindas (primeira utilização)
 *  2. Verificar/configurar a pasta Google Drive para backup de faturas
 *  3. Verificar se o modelo de IA local (Gemma) está disponível ou em download
 *  4. Mostrar feedback quando uma fatura acabou de ser guardada
 *  5. Sincronizar dados de centros de custo com o servidor MariaDB
 *
 * Todos os estados são expostos como [StateFlow] para que o Compose
 * redesenhe automaticamente quando algo muda.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    /** Preferências persistentes da app (SharedPreferences encapsuladas). */
    private val appPreferences: AppPreferences,
    /** Obtém informação do dispositivo (número de série, etc.). */
    private val deviceInfo: DeviceInfo,
    /** Repositório que gere a sincronização de centros de custo com MariaDB. */
    private val frefRepository: FrefRepository,
    /** Repositório de funcionários e presenças (SUBFUNC/SUBFUNC_REG). */
    private val subFuncRepository: SubFuncRepository,
    /** Verifica se o modelo de IA local existe no sistema de ficheiros. */
    private val llmModelManager: LlmModelManager,
    /** Gere o download do modelo de IA via DownloadManager do Android. */
    private val llmModelDownloader: LlmModelDownloader,
    /** DAO de talões — usado para obter o resumo do dia no card do Home. */
    private val talaoDao: TalaoDao
) : ViewModel() {

    // ── Estados observáveis pelo HomeScreen ──────────────────────────────

    /** true se o utilizador nunca viu o diálogo de boas-vindas. */
    private val _mostrarBoasVindas = MutableStateFlow(!appPreferences.jaViuBoasVindas)
    val mostrarBoasVindas: StateFlow<Boolean> = _mostrarBoasVindas.asStateFlow()

    /** true se o utilizador já escolheu uma pasta para backup Drive. */
    private val _driveConfigurado = MutableStateFlow(!appPreferences.driveFolderUri.isNullOrBlank())
    val driveConfigurado: StateFlow<Boolean> = _driveConfigurado.asStateFlow()

    /** Dados do banner "Fatura guardada com sucesso" — null se não há feedback. */
    private val _feedbackUltimaFatura = MutableStateFlow<FeedbackFatura?>(null)
    val feedbackUltimaFatura: StateFlow<FeedbackFatura?> = _feedbackUltimaFatura.asStateFlow()

    /** true se o ficheiro do modelo Gemma existe no disco do dispositivo. */
    private val _modeloIaDisponivel = MutableStateFlow(llmModelManager.modelExists())
    val modeloIaDisponivel: StateFlow<Boolean> = _modeloIaDisponivel.asStateFlow()

    /** Progresso do download do modelo IA (percentagem, estado, erro). */
    private val _downloadProgress = MutableStateFlow(LlmDownloadProgress())
    val downloadProgress: StateFlow<LlmDownloadProgress> = _downloadProgress.asStateFlow()

    /** Estado da ligação ao MariaDB — exibido no chip do HomeScreen. */
    private val _estadoBd = MutableStateFlow(EstadoBd.DESCONHECIDO)
    val estadoBd: StateFlow<EstadoBd> = _estadoBd.asStateFlow()

    /** Mensagem de erro da última tentativa de ligação (null se OK). */
    private val _erroBd = MutableStateFlow<String?>(null)
    val erroBd: StateFlow<String?> = _erroBd.asStateFlow()

    /** Timestamp (millis) da última sync bem-sucedida — para o chip do header. */
    private val _ultimaSync = MutableStateFlow(appPreferences.ultimaSyncTimestamp)
    val ultimaSync: StateFlow<Long> = _ultimaSync.asStateFlow()

    /**
     * Resumo do dia atual — total de talões e despesas de hoje.
     * Observa a BD em tempo real: cada novo talão guardado atualiza o card.
     */
    private val _resumoDia = MutableStateFlow(ResumoDia())
    val resumoDia: StateFlow<ResumoDia> = _resumoDia.asStateFlow()

    /** Resumo semanal e mensal — calculado a partir do resumo por dia. */
    private val _resumoPeriodo = MutableStateFlow(ResumoPeriodo())
    val resumoPeriodo: StateFlow<ResumoPeriodo> = _resumoPeriodo.asStateFlow()

    /** Últimas 5 faturas registadas — para a mini-lista do Home. */
    private val _ultimasFaturas = MutableStateFlow<List<FaturaRecente>>(emptyList())
    val ultimasFaturas: StateFlow<List<FaturaRecente>> = _ultimasFaturas.asStateFlow()

    /** Job de observação do resumo do dia — cancelado em onCleared(). */
    private var resumoDiaJob: Job? = null

    /** Job de observação das últimas faturas — cancelado em onCleared(). */
    private var ultimasFaturasJob: Job? = null

    /** Job do polling de progresso do download — cancelado em onCleared(). */
    private var pollingJob: Job? = null

    init {
        // Se existe um download em curso de uma sessão anterior, retomar o polling
        val downloadIdGuardado = appPreferences.llmDownloadId
        if (downloadIdGuardado >= 0L && !llmModelManager.modelExists()) {
            iniciarPolling(downloadIdGuardado)
        }
        // Tentar sincronizar centros de custo com o servidor
        sincronizarBd()
        // Observar resumo do dia atual em tempo real
        observarResumoDia()
        // Observar últimas faturas
        observarUltimasFaturas()
        // Carregar contagem de presenças de hoje (SUBFUNC_REG via JDBC)
        carregarPresencasHoje()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Resumo do dia atual
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Observa o resumo de todos os dias e calcula:
     *  - Resumo do dia atual
     *  - Resumo da semana (últimos 7 dias)
     *  - Resumo do mês atual
     *
     * Usa a query [TalaoDao.observarResumoPorDia] que já existe —
     * não cria queries novas. Quando um novo talão é guardado,
     * o Room emite uma nova lista e os cards no Home atualizam automaticamente.
     */
    private fun observarResumoDia() {
        val hoje = LocalDate.now()
        val hojeStr = hoje.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val inicioSemana = hoje.minusDays(6) // últimos 7 dias incluindo hoje
        val inicioMes = hoje.withDayOfMonth(1)

        resumoDiaJob?.cancel()
        resumoDiaJob = viewModelScope.launch {
            talaoDao.observarResumoPorDia()
                .collect { lista ->
                    // Resumo do dia
                    val rowHoje = lista.firstOrNull { it.data == hojeStr }
                    _resumoDia.value = _resumoDia.value.copy(
                        totalTaloes   = rowHoje?.totalTaloes ?: 0,
                        totalDespesas = rowHoje?.totalDespesas ?: 0.0
                    )

                    // Resumo semanal e mensal — filtrar por intervalo de datas
                    var taloesSemana = 0; var despesasSemana = 0.0
                    var taloesMes = 0; var despesasMes = 0.0

                    lista.forEach { row ->
                        val dataRow = try {
                            LocalDate.parse(row.data, DateTimeFormatter.ISO_LOCAL_DATE)
                        } catch (_: Exception) { null }

                        if (dataRow != null) {
                            if (!dataRow.isBefore(inicioSemana) && !dataRow.isAfter(hoje)) {
                                taloesSemana += row.totalTaloes
                                despesasSemana += row.totalDespesas ?: 0.0
                            }
                            if (!dataRow.isBefore(inicioMes) && !dataRow.isAfter(hoje)) {
                                taloesMes += row.totalTaloes
                                despesasMes += row.totalDespesas ?: 0.0
                            }
                        }
                    }

                    _resumoPeriodo.value = ResumoPeriodo(
                        totalTaloesSemana   = taloesSemana,
                        totalDespesasSemana = despesasSemana,
                        totalTaloesMes      = taloesMes,
                        totalDespesasMes    = despesasMes
                    )
                }
        }
    }

    /**
     * Observa as 5 faturas mais recentes para a mini-lista do Home.
     * Transforma TalaoEntity em FaturaRecente (dados mínimos para exibição).
     */
    private fun observarUltimasFaturas() {
        ultimasFaturasJob?.cancel()
        ultimasFaturasJob = viewModelScope.launch {
            talaoDao.observarUltimos(5)
                .map { entities ->
                    entities.map { e ->
                        FaturaRecente(
                            id      = e.id,
                            empresa = e.empresa.ifBlank { "Sem empresa" },
                            total   = e.total?.let { "%.2f €".format(it.toDouble()) } ?: "—",
                            data    = e.data?.format(DateTimeFormatter.ofPattern("dd/MM")) ?: "—"
                        )
                    }
                }
                .collect { _ultimasFaturas.value = it }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Presenças de hoje (SUBFUNC_REG)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Consulta a contagem de presenças registadas hoje via JDBC (SUBFUNC_REG).
     * Atualiza o campo [ResumoDia.totalPresencas] no estado.
     */
    private fun carregarPresencasHoje() {
        viewModelScope.launch(Dispatchers.IO) {
            val hojeStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val linhas  = subFuncRepository.consultarPresencas(hojeStr)
            _resumoDia.value = _resumoDia.value.copy(totalPresencas = linhas.size)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sincronização BD
    // ─────────────────────────────────────────────────────────────────────────

    private fun sincronizarBd() {
        viewModelScope.launch(Dispatchers.IO) {
            _estadoBd.value = EstadoBd.A_SINCRONIZAR
            _erroBd.value   = null
            val serial    = deviceInfo.getSerialNumber(
                pt.controleobras.app.ControleObrasApplication.appContext
            )
            val resultado = frefRepository.sincronizar(serial)
            when (resultado) {
                is ResultadoSync.Sucesso -> {
                    _estadoBd.value = EstadoBd.LIGADO
                    _erroBd.value   = null
                    val agora = System.currentTimeMillis()
                    appPreferences.ultimaSyncTimestamp = agora
                    _ultimaSync.value = agora
                }
                is ResultadoSync.Erro -> {
                    _estadoBd.value = EstadoBd.ERRO
                    _erroBd.value   = resultado.mensagem
                }
            }
        }
    }

    fun tentarLigarBd() { sincronizarBd() }

    // ─────────────────────────────────────────────────────────────────────────
    // Boas-vindas
    // ─────────────────────────────────────────────────────────────────────────

    fun fecharBoasVindas() {
        appPreferences.jaViuBoasVindas = true
        _mostrarBoasVindas.value = false
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Google Drive
    // ─────────────────────────────────────────────────────────────────────────

    fun guardarDriveFolderUri(uri: String) {
        appPreferences.driveFolderUri = uri
        _driveConfigurado.value = true
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Feedback — última fatura guardada
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifica se existe feedback de fatura guardada para mostrar no banner.
     *
     * O feedback é guardado pelo ReceiptFlowViewModel ao guardar um talão,
     * no formato "empresa|total|timestamp". O timestamp é verificado para
     * garantir que o banner só aparece se a fatura foi guardada há menos
     * de 30 segundos — evita banners fantasma de sessões anteriores.
     */
    fun verificarFeedbackFatura() {
        // Atualizar presenças ao retomar (pode ter mudado noutro ecrã)
        carregarPresencasHoje()
        val raw = appPreferences.ultimaFaturaFeedback ?: return
        appPreferences.ultimaFaturaFeedback = null
        val partes = raw.split("|")
        if (partes.size >= 3) {
            // Verificar se o feedback ainda é recente (máximo 30 segundos)
            val timestamp = partes[2].toLongOrNull() ?: 0L
            val agora = System.currentTimeMillis()
            val expiracaoMs = 30_000L // 30 segundos
            if (agora - timestamp > expiracaoMs) return // feedback expirado, ignorar

            _feedbackUltimaFatura.value = FeedbackFatura(
                empresa = partes[0],
                total   = partes[1]
            )
        } else if (partes.size >= 2) {
            // Formato antigo sem timestamp — mostrar mesmo assim (retrocompatibilidade)
            _feedbackUltimaFatura.value = FeedbackFatura(
                empresa = partes[0],
                total   = partes[1]
            )
        }
    }

    fun fecharFeedbackFatura() {
        _feedbackUltimaFatura.value = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Modelo IA
    // ─────────────────────────────────────────────────────────────────────────

    fun verificarModeloIa() {
        val existe = llmModelManager.modelExists()
        _modeloIaDisponivel.value = existe
        if (existe) {
            pollingJob?.cancel()
            _downloadProgress.value = LlmDownloadProgress(estado = LlmDownloadEstado.CONCLUIDO)
            appPreferences.llmDownloadId = -1L
        }
    }

    fun iniciarDownloadModelo() {
        if (llmModelManager.modelExists()) return
        if (_downloadProgress.value.estado == LlmDownloadEstado.A_DESCARREGAR) return
        _downloadProgress.value = LlmDownloadProgress(estado = LlmDownloadEstado.A_DESCARREGAR)
        val downloadId = llmModelDownloader.iniciarDownload()
        appPreferences.llmDownloadId = downloadId
        iniciarPolling(downloadId)
    }

    fun cancelarDownload() {
        pollingJob?.cancel()
        val id = appPreferences.llmDownloadId
        if (id >= 0L) {
            llmModelDownloader.cancelar(id)
            appPreferences.llmDownloadId = -1L
        }
        _downloadProgress.value = LlmDownloadProgress(estado = LlmDownloadEstado.IDLE)
    }

    private fun iniciarPolling(downloadId: Long) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                val progress = llmModelDownloader.queryProgress(downloadId)
                _downloadProgress.value = progress
                when (progress.estado) {
                    LlmDownloadEstado.CONCLUIDO -> {
                        _modeloIaDisponivel.value = true
                        appPreferences.llmDownloadId = -1L
                        break
                    }
                    LlmDownloadEstado.ERRO -> {
                        appPreferences.llmDownloadId = -1L
                        break
                    }
                    else -> delay(500)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        resumoDiaJob?.cancel()
        ultimasFaturasJob?.cancel()
    }
}
