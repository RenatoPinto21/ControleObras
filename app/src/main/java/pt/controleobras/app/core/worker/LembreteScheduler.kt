package pt.controleobras.app.core.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Agenda o [LembreteWorker] como trabalho periódico.
 *
 * Executa a cada 4 horas com requisito de conectividade de rede
 * (necessário para consultar presenças via JDBC).
 *
 * Usa [ExistingPeriodicWorkPolicy.KEEP] — se já existe um agendamento
 * ativo, não cria duplicado.
 */
object LembreteScheduler {

    fun agendar(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<LembreteWorker>(
            repeatInterval = 4,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            LembreteWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
