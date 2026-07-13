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
 *
 * O ML Kit devolve a hierarquia TextBlock → TextLine → TextElement,
 * onde cada nível tem um [android.graphics.Rect] com as coordenadas em píxeis.
 * [recognizeStructured] converte essas coordenadas para valores normalizados (0.0–1.0).
 */
class MlKitTextRecognizer @Inject constructor() : TextRecognizer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // ─────────────────────────────────────────────────────────────────────────
    // recognizeText — compatibilidade retroativa
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun recognizeText(context: Context, imageUri: Uri): String =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromFilePath(context, imageUri)
            recognizer.process(image)
                .addOnSuccessListener { visionText -> continuation.resume(visionText.text) }
                .addOnFailureListener { exception -> continuation.resumeWithException(exception) }
        }

    // ─────────────────────────────────────────────────────────────────────────
    // recognizeStructured — texto + posição normalizada por elemento
    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun recognizeStructured(context: Context, imageUri: Uri): StructuredOcrResult =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromFilePath(context, imageUri)
            val imgWidth  = image.width.coerceAtLeast(1)
            val imgHeight = image.height.coerceAtLeast(1)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val elements = mutableListOf<OcrElement>()

                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            for (element in line.elements) {
                                val box = element.boundingBox ?: continue
                                val text = element.text.trim()
                                if (text.isEmpty()) continue

                                elements.add(
                                    OcrElement(
                                        text   = text,
                                        left   = box.left.toFloat()   / imgWidth,
                                        top    = box.top.toFloat()    / imgHeight,
                                        right  = box.right.toFloat()  / imgWidth,
                                        bottom = box.bottom.toFloat() / imgHeight
                                        // confidence: ML Kit 16.x não expõe este valor em Element;
                                        // fica em 1f (default em OcrElement)
                                    )
                                )
                            }
                        }
                    }

                    continuation.resume(
                        StructuredOcrResult(
                            elements    = elements,
                            fullText    = visionText.text,
                            imageWidth  = imgWidth,
                            imageHeight = imgHeight
                        )
                    )
                }
                .addOnFailureListener { exception -> continuation.resumeWithException(exception) }
        }
}
