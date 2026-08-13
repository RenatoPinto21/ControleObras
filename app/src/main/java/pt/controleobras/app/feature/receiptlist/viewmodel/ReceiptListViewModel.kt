package pt.controleobras.app.feature.receiptlist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pt.controleobras.app.core.model.Talao
import pt.controleobras.app.data.repository.TalaoRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel da lista de faturas com suporte a pesquisa e filtros.
 *
 * A filtragem é feita em memória sobre a lista completa de talões
 * (já carregada pelo Room via Flow). Para centenas de registos,
 * a filtragem em memória é instantânea e evita queries SQL complexas.
 *
 * Filtros disponíveis:
 *  - Texto livre: pesquisa por empresa, NIF, número de fatura ou observações
 *  - Data: filtra por um dia específico
 *  - Centro de custo: filtra por código FREF
 *
 * Todos os filtros são combinados com AND — o talão tem de satisfazer
 * TODOS os filtros activos para aparecer na lista.
 */
@HiltViewModel
class ReceiptListViewModel @Inject constructor(
    talaoRepository: TalaoRepository
) : ViewModel() {

    // ── Fonte de dados — todos os talões ─────────────────────────────────
    private val todosOsTalaes = talaoRepository.observarTodos()

    // ── Estado dos filtros ───────────────────────────────────────────────

    /** Texto de pesquisa livre (empresa, NIF, nº fatura, observações). */
    private val _termoPesquisa = MutableStateFlow("")
    val termoPesquisa: StateFlow<String> = _termoPesquisa.asStateFlow()

    /** Filtro por data específica — null = sem filtro de data. */
    private val _filtroData = MutableStateFlow<LocalDate?>(null)
    val filtroData: StateFlow<LocalDate?> = _filtroData.asStateFlow()

    /** Filtro por centro de custo (código FREF) — null = todos. */
    private val _filtroCentroCusto = MutableStateFlow<String?>(null)
    val filtroCentroCusto: StateFlow<String?> = _filtroCentroCusto.asStateFlow()

    // ── Lista filtrada — resultado final exibido no ecrã ─────────────────

    /**
     * Combina a lista completa com os 3 filtros.
     * Cada vez que qualquer filtro muda, o Compose recebe a lista atualizada.
     */
    val talaes: StateFlow<List<Talao>> = combine(
        todosOsTalaes,
        _termoPesquisa,
        _filtroData,
        _filtroCentroCusto
    ) { lista, termo, data, cc ->
        var resultado = lista

        // Filtro por texto — pesquisa case-insensitive em múltiplos campos
        if (termo.isNotBlank()) {
            val termoLower = termo.lowercase()
            resultado = resultado.filter { talao ->
                talao.empresa.lowercase().contains(termoLower) ||
                talao.nif?.lowercase()?.contains(termoLower) == true ||
                talao.numeroFatura?.lowercase()?.contains(termoLower) == true ||
                talao.observacoes?.lowercase()?.contains(termoLower) == true ||
                talao.nmfref.lowercase().contains(termoLower)
            }
        }

        // Filtro por data
        if (data != null) {
            resultado = resultado.filter { it.data == data }
        }

        // Filtro por centro de custo
        if (!cc.isNullOrBlank()) {
            resultado = resultado.filter { it.fref == cc }
        }

        resultado
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Lista de centros de custo únicos presentes nos talões — para popular
     * os chips de filtro. Extraída da lista completa de talões.
     */
    val centrosCustoDisponiveis: StateFlow<List<String>> = todosOsTalaes
        .combine(MutableStateFlow(Unit)) { lista, _ ->
            lista
                .map { it.fref }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Ações ────────────────────────────────────────────────────────────

    fun pesquisar(termo: String) {
        _termoPesquisa.value = termo
    }

    fun filtrarPorData(data: LocalDate?) {
        _filtroData.value = data
    }

    fun filtrarPorCentroCusto(cc: String?) {
        _filtroCentroCusto.value = cc
    }

    /** Limpa todos os filtros de uma só vez. */
    fun limparFiltros() {
        _termoPesquisa.value = ""
        _filtroData.value = null
        _filtroCentroCusto.value = null
    }

    /** true se algum filtro está ativo — para mostrar o botão "limpar". */
    val temFiltrosAtivos: Boolean
        get() = _termoPesquisa.value.isNotBlank() ||
                _filtroData.value != null ||
                !_filtroCentroCusto.value.isNullOrBlank()
}
