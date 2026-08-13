package pt.controleobras.app.core.device

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fornece o identificador do dispositivo (Android ID) e gera o IDREG.
 *
 * Utiliza o ANDROID_ID — estável, único por dispositivo, sem permissões necessárias.
 * Prefixado com "AID_" para clareza nos registos.
 */
@Singleton
class DeviceInfo @Inject constructor() {

    @SuppressLint("HardwareIds")
    fun getSerialNumber(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "0000000000000000"
        return androidId.uppercase()
    }

    /**
     * Gera o ID de registo com base na data/hora atual.
     * Formato: AAAAMMDDHHMMSS
     */
    fun gerarIdReg(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
}
