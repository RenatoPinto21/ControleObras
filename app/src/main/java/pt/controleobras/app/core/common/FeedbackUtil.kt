package pt.controleobras.app.core.common

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService

/**
 * Utilitário para feedback sensorial — vibração e som.
 *
 * Num contexto de obra, o utilizador pode estar com luvas,
 * em ambiente ruidoso ou sem olhar para o ecrã.
 * Uma vibração curta + bip confirma a ação sem depender da visão.
 *
 * Uso típico:
 *   FeedbackUtil.sucessoAoGuardar(context)
 *
 * O volume do bip segue o volume de notificação do sistema —
 * se o dispositivo estiver em silêncio, só vibra.
 */
object FeedbackUtil {

    // ── Duração da vibração em milissegundos ─────────────────────────────
    private const val DURACAO_VIBRACAO_MS = 80L

    // ── Volume do bip (0–100). 60 = audível sem ser agressivo. ──────────
    private const val VOLUME_BIP = 60

    // ── Duração do bip em milissegundos ─────────────────────────────────
    private const val DURACAO_BIP_MS = 120

    /**
     * Feedback de sucesso ao guardar um talão.
     *
     * Executa, por esta ordem:
     *  1. Vibração curta (80 ms)
     *  2. Bip curto (120 ms, tom agudo)
     *
     * Se o dispositivo não tiver vibrador, salta para o bip.
     * Se o volume de notificação estiver a zero, emite apenas vibração.
     *
     * Nunca lança exceção — falhas são silenciadas para não
     * interromper o fluxo principal (o talão já está guardado).
     */
    fun sucessoAoGuardar(context: Context) {
        vibrar(context)
        emitirBip()
    }

    // ── Vibração ─────────────────────────────────────────────────────────

    /**
     * Vibração curta compatível com API 26+ (VibrationEffect) e versões
     * anteriores (método legacy). No API 31+ usa VibratorManager.
     */
    private fun vibrar(context: Context) {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31+ — obter vibrador via VibratorManager
                context.getSystemService<VibratorManager>()?.defaultVibrator
            } else {
                // API < 31 — serviço direto
                @Suppress("DEPRECATION")
                context.getSystemService<Vibrator>()
            }

            vibrator?.let {
                if (!it.hasVibrator()) return

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // API 26+ — efeito com amplitude predefinida
                    it.vibrate(
                        VibrationEffect.createOneShot(
                            DURACAO_VIBRACAO_MS,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    // Fallback legacy (API < 26)
                    @Suppress("DEPRECATION")
                    it.vibrate(DURACAO_VIBRACAO_MS)
                }
            }
        } catch (_: Exception) {
            // Falha silenciosa — o talão já está guardado
        }
    }

    // ── Bip sonoro ──────────────────────────────────────────────────────

    /**
     * Emite um bip curto usando ToneGenerator.
     *
     * Usa o stream NOTIFICATION para respeitar o volume de notificação
     * do sistema. Se o utilizador tiver o dispositivo em silêncio,
     * o ToneGenerator não emite som (comportamento correto).
     *
     * O ToneGenerator é criado e libertado imediatamente — sem fugas
     * de recursos. O tom PROP_BEEP é neutro e profissional.
     */
    private fun emitirBip() {
        try {
            val toneGenerator = ToneGenerator(
                AudioManager.STREAM_NOTIFICATION,
                VOLUME_BIP
            )
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, DURACAO_BIP_MS)

            // Libertar após o tom terminar (margem de segurança de 50 ms)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    toneGenerator.release()
                } catch (_: Exception) {
                    // Já foi libertado
                }
            }, DURACAO_BIP_MS + 50L)
        } catch (_: Exception) {
            // Falha silenciosa — dispositivo pode não suportar tons
        }
    }
}
