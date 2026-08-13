package pt.controleobras.app.core.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import pt.controleobras.app.ControleObrasApplication
import pt.controleobras.app.MainActivity
import pt.controleobras.app.R
import pt.controleobras.app.core.database.dao.TalaoDao
import pt.controleobras.app.core.database.remote.SubFuncRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Worker periódico que verifica se existem lembretes a mostrar:
 *
 *  1. **Presenças não registadas** — se é dia útil e depois das 9h,
 *     e ninguém foi marcado como presente hoje, mostra notificação.
 *
 *  2. **Talões de hoje sem registar** — se o dia já começou e ainda
 *     não há talões registados, lembra para digitalizar faturas.
 *
 * Corre a cada ~4 horas (definido pelo agendador).
 * Só mostra notificações em dias úteis (seg–sex).
 */
@HiltWorker
class LembreteWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val subFuncRepository: SubFuncRepository,
    private val talaoDao: TalaoDao
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "LembreteWorker"
        const val WORK_NAME = "lembrete_diario"
        private const val NOTIF_ID_PRESENCAS = 1001
        private const val NOTIF_ID_TALOES    = 1002
    }

    override suspend fun doWork(): Result {
        val hoje = LocalDate.now()
        val agora = LocalTime.now()

        // Só notificar em dias úteis (seg=1 .. sex=5)
        val diaSemana = hoje.dayOfWeek.value
        if (diaSemana > 5) {
            Log.d(TAG, "Fim de semana — sem lembretes")
            return Result.success()
        }

        // Só notificar depois das 9h
        if (agora.hour < 9) {
            Log.d(TAG, "Antes das 9h — sem lembretes")
            return Result.success()
        }

        val hojeStr = hoje.format(DateTimeFormatter.ISO_LOCAL_DATE)

        // ── Verificar presenças ─────────────────────────────────────────────
        try {
            val presencas = subFuncRepository.consultarPresencas(hojeStr)
            if (presencas.isEmpty() && agora.hour >= 9) {
                mostrarNotificacao(
                    id     = NOTIF_ID_PRESENCAS,
                    titulo = "Presenças por registar",
                    texto  = "Ainda não registou presenças hoje. Toque para abrir a app."
                )
                Log.d(TAG, "Notificação: presenças não registadas")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao verificar presenças: ${e.message}")
        }

        // ── Verificar talões ────────────────────────────────────────────────
        try {
            val taloes = talaoDao.contarPorData(hojeStr)
            if (taloes == 0 && agora.hour >= 10) {
                mostrarNotificacao(
                    id     = NOTIF_ID_TALOES,
                    titulo = "Sem despesas registadas",
                    texto  = "Nenhuma fatura digitalizada hoje. Não se esqueça de registar os talões."
                )
                Log.d(TAG, "Notificação: sem talões hoje")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao verificar talões: ${e.message}")
        }

        return Result.success()
    }

    private fun mostrarNotificacao(id: Int, titulo: String, texto: String) {
        // Verificar permissão POST_NOTIFICATIONS (Android 13+)
        if (ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Sem permissão POST_NOTIFICATIONS — notificação omitida")
            return
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(
            applicationContext,
            ControleObrasApplication.CANAL_LEMBRETES_ID
        )
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(id, notif)
    }
}
