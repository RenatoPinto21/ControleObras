package pt.controleobras.app.feature.presencas.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.controleobras.app.core.database.remote.FrefRepository
import pt.controleobras.app.core.database.remote.ResultadoSync
import pt.controleobras.app.core.database.remote.SubFuncRepository
import pt.controleobras.app.core.device.DeviceInfo
import pt.controleobras.app.core.model.CentroCusto
import pt.controleobras.app.core.model.Funcionario
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel do ecrã de Presenças.
 *
 * Gere:
 *   - Dropdown de centros de custo
 *   - Lista de funcionários do CC selecionado
 *   - Seleção presente/ausente
 *   - Envio de presenças para SUBFUNC_REG
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PresencasViewModel @Inject constructor(
    application: Application,
    frefRepository: FrefRepository,
    private val subFuncRepository: SubFuncRepository,
    private val deviceInfo: DeviceInfo
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PresencasVM"
    }

    /** Lista de centros de custo sincronizados — alimenta o dropdown. */
    val centrosCusto: StateFlow<List<CentroCusto>> = frefRepository.listarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** FREF do centro de custo selecionado no dropdown. */
    private val _frefSelecionado = MutableStateFlow("")

    /** Lista de funcionários do CC selecionado (reage a mudanças de _frefSelecionado). */
    val funcionarios: StateFlow<List<Funcionario>> = _frefSelecionado
        .flatMapLatest { fref ->
            if (fref.isBlank()) flowOf(emptyList())
            else subFuncRepository.listarPorFref(fref)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Set de BISTAMPs dos funcionários marcados como presentes. */
    private val _presentes = MutableStateFlow<Set<String>>(emptySet())
    val presentes: StateFlow<Set<String>> = _presentes

    /** Estado de carregamento (sync em curso). */
    private val _carregando = MutableStateFlow(false)
    val carregando: StateFlow<Boolean> = _carregando

    /** Estado de envio (registo em curso). */
    private val _enviando = MutableStateFlow(false)
    val enviando: StateFlow<Boolean> = _enviando

    /** Mensagem de feedback após ação. */
    private val _mensagem = MutableStateFlow<String?>(null)
    val mensagem: StateFlow<String?> = _mensagem

    /** Observações globais para o registo de presenças. */
    private val _observacoes = MutableStateFlow("")
    val observacoes: StateFlow<String> = _observacoes

    /** Observações por empresa (chave = nome da empresa/grupo). */
    private val _obsPorEmpresa = MutableStateFlow<Map<String, String>>(emptyMap())
    val obsPorEmpresa: StateFlow<Map<String, String>> = _obsPorEmpresa

    /**
     * Seleciona um centro de custo e sincroniza os funcionários.
     */
    fun selecionarCentroCusto(fref: String) {
        _frefSelecionado.value = fref
        _presentes.value = emptySet()
        _observacoes.value = ""
        _obsPorEmpresa.value = emptyMap()
        _mensagem.value = null

        viewModelScope.launch {
            _carregando.value = true
            val resultado = subFuncRepository.sincronizarFuncionarios(fref)
            _carregando.value = false

            when (resultado) {
                is ResultadoSync.Sucesso -> {
                    Log.d(TAG, "Carregados ${resultado.total} funcionários para CC $fref")
                    if (resultado.total == 0) {
                        _mensagem.value = "Sem funcionários associados a este centro de custo"
                    }
                }
                is ResultadoSync.Erro -> {
                    _mensagem.value = "Erro ao carregar funcionários: ${resultado.mensagem}"
                }
            }
        }
    }

    /** Alterna a presença de um funcionário. */
    fun alternarPresenca(bistamp: String) {
        _presentes.value = _presentes.value.toMutableSet().apply {
            if (contains(bistamp)) remove(bistamp) else add(bistamp)
        }
    }

    /** Seleciona todos os funcionários como presentes. */
    fun selecionarTodos(funcionarios: List<Funcionario>) {
        _presentes.value = funcionarios.map { it.bistamp }.toSet()
    }

    /** Remove todas as seleções. */
    fun limparSelecao() {
        _presentes.value = emptySet()
    }

    /** Atualiza observações globais. */
    fun atualizarObservacoes(obs: String) {
        _observacoes.value = obs
    }

    /** Atualiza observações de uma empresa específica. */
    fun atualizarObsEmpresa(empresa: String, obs: String) {
        _obsPorEmpresa.value = _obsPorEmpresa.value.toMutableMap().apply {
            if (obs.isBlank()) remove(empresa) else put(empresa, obs)
        }
    }

    /** Limpa mensagem de feedback. */
    fun limparMensagem() {
        _mensagem.value = null
    }

    /**
     * Regista as presenças selecionadas no SUBFUNC_REG.
     */
    fun registarPresencas(funcionariosList: List<Funcionario>) {
        val selecionados = funcionariosList.filter { _presentes.value.contains(it.bistamp) }
        if (selecionados.isEmpty()) {
            _mensagem.value = "Selecione pelo menos um funcionário"
            return
        }

        viewModelScope.launch {
            _enviando.value = true
            val encserie = deviceInfo.getSerialNumber(getApplication())
            val data = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

            // Obs por empresa → obs por bistamp (nome = chave do grupo/empresa)
            val obsEmpresa = _obsPorEmpresa.value
            val obsPorBistamp = if (obsEmpresa.isNotEmpty()) {
                selecionados.associate { func ->
                    func.bistamp to (obsEmpresa[func.nome]?.takeIf { it.isNotBlank() } ?: "")
                }
            } else emptyMap()

            val resultado = subFuncRepository.registarPresencas(
                encserie      = encserie,
                presencas     = selecionados,
                data          = data,
                hora          = hora,
                obs           = _observacoes.value,
                obsPorBistamp = obsPorBistamp
            )
            _enviando.value = false

            when (resultado) {
                is ResultadoSync.Sucesso -> {
                    if (resultado.total > 0) {
                        _mensagem.value = "Registadas ${resultado.total} presenças com sucesso"
                        _presentes.value = emptySet()
                        _observacoes.value = ""
                        _obsPorEmpresa.value = emptyMap()
                    } else {
                        _mensagem.value = "Funcionários selecionados já estavam registados hoje"
                    }
                }
                is ResultadoSync.Erro -> {
                    _mensagem.value = "Erro ao registar presenças: ${resultado.mensagem}"
                }
            }
        }
    }
}
