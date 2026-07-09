package pt.controleobras.app.core.device

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.net.NetworkInterface
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fornece informação estável do dispositivo: MAC address e geração de IDREG.
 *
 * Android 10+ proíbe a leitura direta do MAC Wi-Fi (retorna 02:00:00:00:00:00).
 * Tentamos eth0/wlan0 via NetworkInterface — funciona em alguns tablets de empresa
 * com configurações de privacidade menos restritivas. Fallback: ANDROID_ID
 * formatado como endereço MAC (estável por dispositivo + signing key).
 */
@Singleton
class DeviceInfo @Inject constructor() {

    /**
     * Endereço MAC do dispositivo ou ID formatado como MAC.
     * Formato: "AA:BB:CC:DD:EE:FF" em maiúsculas.
     */
    fun getMacAddress(context: Context): String {
        // Tentativa 1: interface de rede real (funciona em alguns tablets)
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return fallbackId(context)
            for (intf in interfaces) {
                if (intf.name.equals("eth0", ignoreCase = true) ||
                    intf.name.equals("wlan0", ignoreCase = true)
                ) {
                    val mac = intf.hardwareAddress ?: continue
                    val macStr = mac.joinToString(":") { "%02X".format(it) }
                    // Filtra MACs aleatorizados (todos zeros ou 02:00:00:00:00:00)
                    if (macStr != "00:00:00:00:00:00" && macStr != "02:00:00:00:00:00") {
                        return macStr
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback: ANDROID_ID formatado como MAC
        return fallbackId(context)
    }

    /**
     * Gera o ID de registo com base na data/hora atual.
     * Formato: AAAAMMDDHHMMSS
     */
    fun gerarIdReg(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

    @SuppressLint("HardwareIds")
    private fun fallbackId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "0000000000000000"
        // Formata os primeiros 12 chars como MAC: AABBCCDDEEFF → AA:BB:CC:DD:EE:FF
        val padded = androidId.padEnd(12, '0').take(12).uppercase()
        return padded.chunked(2).joinToString(":")
    }
}
