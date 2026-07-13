package pt.controleobras.app.core.ocr

import android.content.Context
import android.net.Uri

/**
 * Abstrai o motor de OCR usado pela aplicação. Isola o resto do código de
 * detalhes do ML Kit, permitindo trocar o motor no futuro sem tocar nas features.
 *
 * Dois modos de operação:
 *  - [recognizeText]       — texto concatenado, compatibilidade retroativa
 *  - [recognizeStructured] — texto + coordenadas normalizadas por elemento
 */
interface TextRecognizer {
    /** Devolve o texto completo da imagem (sem coordenadas). */
    suspend fun recognizeText(context: Context, imageUri: Uri): String

    /**
     * Devolve texto completo + posição normalizada (0.0–1.0) de cada elemento.
     * Usado pelo [pt.controleobras.app.core.extractor.PositionAwareReceiptExtractor].
     */
    suspend fun recognizeStructured(context: Context, imageUri: Uri): StructuredOcrResult
}
