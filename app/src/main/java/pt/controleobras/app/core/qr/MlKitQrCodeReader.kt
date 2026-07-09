package pt.controleobras.app.core.qr

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Leitura de QR code com ML Kit Barcode Scanning (modelo bundled — offline).
 *
 * A imagem é lida via [BitmapFactory] para evitar problemas de rotação EXIF
 * que [InputImage.fromFilePath] pode ter com URIs do FileProvider/CameraX.
 * O scanner é configurado apenas para QR_CODE (mais rápido que detetar todos
 * os formatos).
 */
class MlKitQrCodeReader @Inject constructor() : QrCodeReader {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    override suspend fun readQrCode(context: Context, imageUri: Uri): String? =
        suspendCancellableCoroutine { continuation ->
            runCatching {
                // Ler bitmap via ContentResolver — funciona com qualquer URI
                // (content://, file://, FileProvider, CameraX output)
                val bitmap = if (imageUri.scheme == "content") {
                    context.contentResolver.openInputStream(imageUri)
                        ?.use { BitmapFactory.decodeStream(it) }
                } else {
                    BitmapFactory.decodeFile(imageUri.path)
                } ?: throw IllegalStateException("Não foi possível descodificar a imagem para QR")

                InputImage.fromBitmap(bitmap, 0)
            }.fold(
                onSuccess = { image ->
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val qrValue = barcodes.firstOrNull()?.rawValue
                            continuation.resume(qrValue)
                        }
                        .addOnFailureListener { ex ->
                            continuation.resumeWithException(ex)
                        }
                },
                onFailure = { ex ->
                    continuation.resumeWithException(ex)
                }
            )
        }
}
