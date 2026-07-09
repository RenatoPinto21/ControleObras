package pt.controleobras.app.core.qr

import android.content.Context
import android.net.Uri

/**
 * Abstrai o motor de leitura de QR code.
 * Devolve o conteúdo textual do primeiro QR code encontrado na imagem,
 * ou null se não existir nenhum.
 */
interface QrCodeReader {
    suspend fun readQrCode(context: Context, imageUri: Uri): String?
}
