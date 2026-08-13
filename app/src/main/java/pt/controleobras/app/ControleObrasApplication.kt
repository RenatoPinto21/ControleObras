package pt.controleobras.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import pt.controleobras.app.core.worker.LembreteScheduler
import javax.inject.Inject

/**
 * Ponto de entrada do grafo de injeção de dependências do Hilt.
 *
 * Implementa [Configuration.Provider] para integrar WorkManager com Hilt
 * (permite uso de @HiltWorker nos workers).
 *
 * Expõe [appContext] para uso em singletons que não recebem Context via Hilt
 * (ex: HomeViewModel.sincronizarBd que corre em Dispatchers.IO).
 */
@HiltAndroidApp
class ControleObrasApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        criarCanaisNotificacao()
        LembreteScheduler.agendar(this)
    }

    /**
     * Cria canais de notificação obrigatórios (Android 8+).
     * Chamado uma vez no início — idempotente (recria sem problema).
     */
    private fun criarCanaisNotificacao() {
        val manager = getSystemService(NotificationManager::class.java) ?: return

        val canalLembretes = NotificationChannel(
            CANAL_LEMBRETES_ID,
            "Lembretes",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Lembretes de presenças e talões pendentes"
        }

        manager.createNotificationChannel(canalLembretes)
    }

    companion object {
        lateinit var appContext: Context
            private set

        /** ID do canal de notificações de lembretes. */
        const val CANAL_LEMBRETES_ID = "lembretes_controle_obras"
    }
}
