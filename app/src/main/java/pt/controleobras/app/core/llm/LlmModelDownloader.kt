package pt.controleobras.app.core.llm

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Faz o download do modelo LLM usando o [DownloadManager] do Android.
 *
 * Vantagens sobre HTTP manual:
 *  - Continua em background mesmo com a app fechada
 *  - Retoma automaticamente em caso de falha de rede
 *  - Mostra notificação de progresso no sistema
 *  - Permite download em modo avião → WiFi automático quando disponível
 *
 * O ficheiro é guardado diretamente na pasta esperada por [LlmModelManager]:
 *   Android/data/pt.controleobras.app/files/llm/
 */
@Singleton
class LlmModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelManager: LlmModelManager
) {
    /**
     * Inicia o download do modelo e devolve o ID atribuído pelo DownloadManager.
     * Guardar este ID em [AppPreferences] para poder monitorizar o progresso
     * mesmo após reiniciar a app.
     *
     * Requer permissão INTERNET (já declarada no manifesto).
     */
    fun iniciarDownload(): Long {
        return runCatching {
            // Garantir que a pasta de destino existe
            modelManager.modelDir.mkdirs()

            val request = DownloadManager.Request(Uri.parse(LlmModelManager.MODEL_URL))
                .setTitle("Controle Obras — Modelo IA")
                .setDescription("A descarregar Gemma 2B (${LlmModelManager.MODEL_SIZE_DISPLAY})...")
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                // Destino: pasta externa da app (acessível sem permissões extra)
                .setDestinationInExternalFilesDir(
                    context,
                    "llm",
                    LlmModelManager.MODEL_FILENAME
                )
                // Permitir WiFi e dados móveis (sem restrição de rede)
                .setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or
                        DownloadManager.Request.NETWORK_MOBILE
                )
                // Permitir download mesmo em roaming
                .setAllowedOverRoaming(true)
                // Não mostrar na galeria
                .setVisibleInDownloadsUi(false)

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
        }.getOrDefault(-1L)
    }

    /**
     * Consulta o progresso de um download em curso.
     * Deve ser chamado periodicamente (ex: cada 500ms) enquanto o estado for [LlmDownloadEstado.A_DESCARREGAR].
     */
    fun queryProgress(downloadId: Long): LlmDownloadProgress {
        if (downloadId < 0L) return LlmDownloadProgress()

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = dm.query(query)

        if (!cursor.moveToFirst()) {
            cursor.close()
            return LlmDownloadProgress(estado = LlmDownloadEstado.IDLE)
        }

        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
        val bytesDescarregados = cursor.getLong(
            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
        )
        val bytesTotal = cursor.getLong(
            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
        )
        cursor.close()

        val percentagem = if (bytesTotal > 0)
            ((bytesDescarregados.toDouble() / bytesTotal) * 100).toInt()
        else -1

        return when (status) {
            DownloadManager.STATUS_RUNNING,
            DownloadManager.STATUS_PENDING -> LlmDownloadProgress(
                estado = LlmDownloadEstado.A_DESCARREGAR,
                percentagem = percentagem,
                bytesDescarregados = bytesDescarregados,
                bytesTotal = bytesTotal
            )
            DownloadManager.STATUS_PAUSED -> {
                // PAUSED_WAITING_FOR_NETWORK (reason=3) — sem rede disponível de momento,
                // o DownloadManager vai retomar automaticamente quando a rede voltar.
                // Tratamos como "a descarregar" para não mostrar erro prematuramente.
                LlmDownloadProgress(
                    estado = LlmDownloadEstado.A_DESCARREGAR,
                    percentagem = percentagem,
                    bytesDescarregados = bytesDescarregados,
                    bytesTotal = bytesTotal
                )
            }
            DownloadManager.STATUS_SUCCESSFUL -> LlmDownloadProgress(
                estado = LlmDownloadEstado.CONCLUIDO,
                percentagem = 100,
                bytesDescarregados = bytesTotal,
                bytesTotal = bytesTotal
            )
            DownloadManager.STATUS_FAILED -> LlmDownloadProgress(
                estado = LlmDownloadEstado.ERRO,
                mensagemErro = "Código de erro: $reason"
            )
            else -> LlmDownloadProgress(estado = LlmDownloadEstado.IDLE)
        }
    }

    /**
     * Cancela um download em curso e remove o ficheiro parcial.
     */
    fun cancelar(downloadId: Long) {
        if (downloadId < 0L) return
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.remove(downloadId)
        // Remover ficheiro parcial, se existir
        runCatching { modelManager.modelFile.delete() }
    }
}
