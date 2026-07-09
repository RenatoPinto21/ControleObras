package pt.controleobras.app.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.controleobras.app.core.common.AppPreferences
import pt.controleobras.app.core.llm.LlmDownloadEstado
import pt.controleobras.app.core.llm.LlmDownloadProgress
import pt.controleobras.app.core.llm.LlmModelDownloader
import pt.controleobras.app.core.llm.LlmModelManager
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val llmModelManager: LlmModelManager,
    private val llmModelDownloader: LlmModelDownloader
) : ViewModel() {

    private val _mostrarBoasVindas = MutableStateFlow(!appPreferences.jaViuBoasVindas)
    val mostrarBoasVindas: StateFlow<Boolean> = _mostrarBoasVindas.asStateFlow()

    private val _driveConfigurado = MutableStateFlow(!appPreferences.driveFolderUri.isNullOrBlank())
    val driveConfigurado: StateFlow<Boolean> = _driveConfigurado.asStateFlow()

    private val _modeloIaDisponivel = MutableStateFlow(llmModelManager.modelExists())
    val modeloIaDisponivel: StateFlow<Boolean> = _modeloIaDisponivel.asStateFlow()

    private val _downloadProgress = MutableStateFlow(LlmDownloadProgress())
    val downloadProgress: StateFlow<LlmDownloadProgress> = _downloadProgress.asStateFlow()

    private var pollingJob: Job? = null

    init {
        // Se havia um download em curso quando a app foi fechada, retomar monitorização
        val downloadIdGuardado = appPreferences.llmDownloadId
        if (downloadIdGuardado >= 0L && !llmModelManager.modelExists()) {
            iniciarPolling(downloadIdGuardado)
        }
    }

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
    // Modelo IA — verificação
    // ─────────────────────────────────────────────────────────────────────────

    /** Chamado quando o utilizador volta ao HomeScreen (onResume). */
    fun verificarModeloIa() {
        val existe = llmModelManager.modelExists()
        _modeloIaDisponivel.value = existe
        if (existe) {
            // Download concluído (possivelmente via cópia manual) — limpar estado
            pollingJob?.cancel()
            _downloadProgress.value = LlmDownloadProgress(estado = LlmDownloadEstado.CONCLUIDO)
            appPreferences.llmDownloadId = -1L
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Modelo IA — download automático
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inicia o download do modelo Gemma 2B via DownloadManager.
     * O download continua em background mesmo com a app fechada.
     */
    fun iniciarDownloadModelo() {
        if (llmModelManager.modelExists()) return
        if (_downloadProgress.value.estado == LlmDownloadEstado.A_DESCARREGAR) return

        _downloadProgress.value = LlmDownloadProgress(estado = LlmDownloadEstado.A_DESCARREGAR)

        val downloadId = llmModelDownloader.iniciarDownload()
        appPreferences.llmDownloadId = downloadId
        iniciarPolling(downloadId)
    }

    /** Cancela o download em curso. */
    fun cancelarDownload() {
        pollingJob?.cancel()
        val id = appPreferences.llmDownloadId
        if (id >= 0L) {
            llmModelDownloader.cancelar(id)
            appPreferences.llmDownloadId = -1L
        }
        _downloadProgress.value = LlmDownloadProgress(estado = LlmDownloadEstado.IDLE)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Polling de progresso (corre a cada 500ms enquanto descarrega)
    // ─────────────────────────────────────────────────────────────────────────

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
    }
}
