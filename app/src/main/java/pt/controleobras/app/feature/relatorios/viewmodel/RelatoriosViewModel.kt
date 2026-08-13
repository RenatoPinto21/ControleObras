package pt.controleobras.app.feature.relatorios.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.controleobras.app.core.database.dao.TalaoDao
import pt.controleobras.app.core.database.remote.FrefRepository
import pt.controleobras.app.core.database.remote.SubFuncRepository
import pt.controleobras.app.core.model.CentroCusto
import pt.controleobras.app.core.relatorios.model.DiaResumo
import pt.controleobras.app.core.relatorios.model.LinhaDespesa
import pt.controleobras.app.core.relatorios.model.LinhaPresenca
import pt.controleobras.app.core.relatorios.model.LinhaPresencaReg
import pt.controleobras.app.core.relatorios.model.RelatorioDespesas
import pt.controleobras.app.core.relatorios.model.RelatorioPresencas
import pt.controleobras.app.core.relatorios.model.RelatorioPresencasReg
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Tipos auxiliares do ecrã de Relatórios
// ─────────────────────────────────────────────────────────────────────────────

/** Define qual painel está visível no lado direito: despesas ou presenças. */
enum class PainelAtivo { DESPESAS, PRESENCAS }

/**
 * Estado completo do ecrã de relatórios.
 *
 * O Compose observa este objeto e redesenha automaticamente quando algo muda.
 * Todos os campos têm valores por defeito para que o ecrã abra sem erros.
 */
data class RelatoriosUiState(
    /** Mês que o calendário está a mostrar (ex: julho 2026). */
    val mesSelecionado: YearMonth        = YearMonth.now(),
    /** Dia que o utilizador clicou no calendário (destaca-se a azul). */
    val diaSelecionado: LocalDate?       = LocalDate.now(),
    /** Qual painel está visível: DESPESAS ou PRESENCAS. */
    val painelAtivo:    PainelAtivo      = PainelAtivo.DESPESAS,
    /** Mapa dia→resumo para pintar indicadores no calendário (pontos, totais). */
    val resumosDias:    Map<LocalDate, DiaResumo> = emptyMap(),
    /** Relatório de despesas do dia selecionado (lista de faturas + total). */
    val despesas:       RelatorioDespesas? = null,
    /** Relatório de presenças do dia selecionado (funcionários únicos — via talões). */
    val presencas:      RelatorioPresencas? = null,
    /** Relatório de presenças registadas via SUBFUNC_REG (MariaDB). */
    val presencasReg:   RelatorioPresencasReg? = null,
    /** Lista de centros de custo disponíveis para filtro de presenças. */
    val centrosCusto:   List<CentroCusto> = emptyList(),
    /** CC selecionado no filtro de presenças (null = todos). */
    val ccFiltro:       String?          = null,
    /** true enquanto os dados do dia estão a ser carregados da BD. */
    val aCarregar:      Boolean          = false
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ViewModel do ecrã de Relatórios.
 *
 * Responsabilidades:
 *  1. Observar os resumos de todos os dias (para os indicadores do calendário)
 *  2. Quando o utilizador clica num dia, carregar despesas e presenças desse dia
 *  3. Permitir trocar de mês e de painel (despesas/presenças)
 *
 * NOTA IMPORTANTE sobre o carregamento de dias:
 *   Cada dia usa um Flow do Room (observarPorData) que fica ativo infinitamente.
 *   Se o utilizador clica em 5 dias seguidos, são criados 5 collectors — e os 5
 *   escrevem no mesmo _uiState.despesas, causando conflitos.
 *   Para evitar isso, guardamos o Job em [despesasJob] e cancelamos o anterior
 *   antes de lançar um novo. O mesmo para presenças em [presencasJob].
 */
@HiltViewModel
class RelatoriosViewModel @Inject constructor(
    private val talaoDao: TalaoDao,
    private val subFuncRepository: SubFuncRepository,
    private val frefRepository: FrefRepository
) : ViewModel() {

    /** Formato ISO usado para converter LocalDate ↔ String na BD (ex: "2026-07-23"). */
    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    private val _uiState = MutableStateFlow(RelatoriosUiState())
    val uiState: StateFlow<RelatoriosUiState> = _uiState.asStateFlow()

    /**
     * Job da coroutine que observa as despesas do dia selecionado.
     * Cancelado cada vez que o utilizador muda de dia — evita leak de collectors.
     */
    private var despesasJob: Job? = null

    /**
     * Job da coroutine que carrega as presenças do dia selecionado.
     * Embora presenças use suspend (não Flow), mantemos o Job por consistência
     * e para poder cancelar se o utilizador mudar de dia muito rápido.
     */
    private var presencasJob: Job? = null

    /** Job da consulta de presenças registadas (SUBFUNC_REG via JDBC). */
    private var presencasRegJob: Job? = null

    init {
        // ── Observar resumos globais ─────────────────────────────────────────
        viewModelScope.launch {
            talaoDao.observarResumoPorDia().collect { rows ->
                val resumoMap = mutableMapOf<LocalDate, DiaResumo>()
                rows.forEach { row ->
                    runCatching { LocalDate.parse(row.data, fmt) }.getOrNull()?.let { data ->
                        resumoMap[data] = DiaResumo(
                            data           = data,
                            totalTaloes    = row.totalTaloes,
                            totalDespesas  = row.totalDespesas,
                            totalPresencas = 0
                        )
                    }
                }
                _uiState.value = _uiState.value.copy(resumosDias = resumoMap)
            }
        }

        // ── Observar lista de centros de custo (para filtro de presenças) ────
        viewModelScope.launch {
            frefRepository.listarTodos().collect { lista ->
                _uiState.value = _uiState.value.copy(centrosCusto = lista)
            }
        }

        // Carregar os dados do dia de hoje ao abrir o ecrã
        carregarDia(LocalDate.now())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ações do utilizador (chamadas pela UI)
    // ─────────────────────────────────────────────────────────────────────────

    /** Chamado quando o utilizador clica num dia do calendário. */
    fun selecionarDia(dia: LocalDate) {
        _uiState.value = _uiState.value.copy(diaSelecionado = dia)
        carregarDia(dia)
    }

    /** Chamado quando o utilizador navega para outro mês (setas ◄ ►). */
    fun selecionarMes(mes: YearMonth) {
        _uiState.value = _uiState.value.copy(mesSelecionado = mes)
    }

    /** Chamado quando o utilizador troca entre o painel de despesas e presenças. */
    fun selecionarPainel(painel: PainelAtivo) {
        _uiState.value = _uiState.value.copy(painelAtivo = painel)
    }

    /**
     * Filtra presenças registadas por centro de custo.
     * @param fref Código do CC (null = mostrar todos).
     */
    fun filtrarPresencasPorCC(fref: String?) {
        _uiState.value = _uiState.value.copy(ccFiltro = fref)
        // Recarregar presenças registadas com o novo filtro
        _uiState.value.diaSelecionado?.let { dia -> carregarPresencasReg(dia, fref) }
    }

    /**
     * Força o recarregamento do dia selecionado.
     * Útil quando o utilizador acabou de submeter uma nova fatura e quer
     * ver os dados atualizados sem ter de clicar novamente no dia.
     */
    fun refreshDiaAtual() {
        val dia = _uiState.value.diaSelecionado ?: LocalDate.now()
        carregarDia(dia)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Carregamento de dados do dia
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Carrega as despesas e presenças de um dia específico.
     *
     * IMPORTANTE: Cancela os jobs anteriores antes de lançar novos.
     * Sem isto, cada clique num dia criava um collector infinito adicional,
     * e vários collectors escreviam no mesmo campo _uiState.despesas ao
     * mesmo tempo — o utilizador via dados de dias diferentes a "piscar".
     */
    private fun carregarDia(dia: LocalDate) {
        val dataStr = dia.format(fmt)

        // ── Cancelar collectors anteriores ───────────────────────────────────
        despesasJob?.cancel()
        presencasJob?.cancel()
        presencasRegJob?.cancel()

        // ── Despesas (Flow — fica a observar alterações em tempo real) ────────
        despesasJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(aCarregar = true)

            talaoDao.observarPorData(dataStr).collect { entidades ->
                val linhas = entidades.map { t ->
                    LinhaDespesa(
                        id           = t.id,
                        hora         = t.hora?.toString() ?: "--:--",
                        empresa      = t.empresa,
                        nif          = t.nif.orEmpty(),
                        numeroFatura = t.numeroFatura.orEmpty(),
                        total        = t.total?.toDouble() ?: 0.0,
                        funcn        = t.funcn,
                        fref         = t.fref,
                        nmfref       = t.nmfref
                    )
                }
                val totalGeral = linhas.sumOf { it.total }
                _uiState.value = _uiState.value.copy(
                    despesas  = RelatorioDespesas(dia, linhas, totalGeral),
                    aCarregar = false
                )
            }
        }

        // ── Presenças via talões (Room — mantido para compatibilidade) ────────
        presencasJob = viewModelScope.launch {
            val rows = talaoDao.obterPresencasDia(dataStr)
            val linhas = rows.map { r ->
                LinhaPresenca(
                    funcn       = r.funcn,
                    fref        = r.fref,
                    nmfref      = r.nmfref,
                    agnome      = r.agnome,
                    totalTaloes = r.totalTaloes
                )
            }
            _uiState.value = _uiState.value.copy(
                presencas = RelatorioPresencas(dia, linhas)
            )
        }

        // ── Presenças registadas (SUBFUNC_REG via JDBC) ──────────────────────
        carregarPresencasReg(dia, _uiState.value.ccFiltro)
    }

    /**
     * Carrega presenças registadas do SUBFUNC_REG para um dia e CC opcional.
     * Separado de [carregarDia] para poder ser chamado isoladamente ao mudar filtro CC.
     */
    private fun carregarPresencasReg(dia: LocalDate, fref: String?) {
        presencasRegJob?.cancel()
        presencasRegJob = viewModelScope.launch {
            val dataStr = dia.format(fmt)
            val linhas  = subFuncRepository.consultarPresencas(dataStr, fref)
            _uiState.value = _uiState.value.copy(
                presencasReg = RelatorioPresencasReg(
                    data       = dia,
                    linhas     = linhas,
                    frefFiltro = fref
                )
            )
        }
    }
}
