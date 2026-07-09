package pt.controleobras.app.core.ocr

import android.content.Context
import android.net.Uri

/**
 * Abstrai o motor de OCR usado pela aplicação. Isola o resto do código de
 * detalhes do ML Kit, permitindo trocar o motor no futuro sem tocar nas features.
 */
interface TextRecognizer {
    suspend fun recognizeText(context: Context, imageUri: Uri): String
}
