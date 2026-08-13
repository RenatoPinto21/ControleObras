package pt.controleobras.app.core.model

/**
 * Metadados técnicos recolhidos no momento da captura da imagem.
 * Usados para compor o CSV de registo e para nomear os ficheiros gerados.
 *
 * @param serialDispositivo  Número de série do dispositivo (Build.getSerial() ou "AID_{ANDROID_ID}")
 * @param idReg              Timestamp no formato AAAAMMDDHHMMSS — identifica este registo
 * @param gps                Coordenadas GPS no momento da captura ("lat,lon") ou vazio se não disponível
 * @param qrCodeRaw          Conteúdo bruto do QR code AT, ou null se não detetado
 */
data class CaptureMetadata(
    val serialDispositivo: String,
    val idReg: String,
    val gps: String,
    val qrCodeRaw: String?
) {
    /** Nome base dos ficheiros gerados: {SERIAL}_{IDREG} */
    val fileBaseName: String
        get() = "${serialDispositivo.replace(":", "_")}_$idReg"
}
