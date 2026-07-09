package pt.controleobras.app.core.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementação com Google ML Kit (modelo "bundled" — corre no dispositivo,
 * sem necessitar de rede após a instalação da app).
 */
class MlKitTextRecognizer @Inject constructor() : TextRecognizer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognizeText(context: Context, imageUri: Uri): String =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromFilePath(context, imageUri)
            recognizer.process(image)
                .addOnSuccessListener { visionText -> continuation.resume(visionText.text) }
                .addOnFailureListener { exception -> continuation.resumeWithException(exception) }
        }
}
