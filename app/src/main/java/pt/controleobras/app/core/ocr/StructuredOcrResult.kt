package pt.controleobras.app.core.ocr

/**
 * Resultado estruturado do OCR — preserva texto + posição normalizada (0.0–1.0)
 * de cada palavra na imagem.
 *
 * As coordenadas são normalizadas pela largura/altura da imagem original,
 * tornando-as independentes da resolução do tablet.
 */
data class StructuredOcrResult(
    /** Todos os elementos de texto com as suas posições na imagem. */
    val elements: List<OcrElement>,
    /** Texto completo concatenado (compatibilidade com o pipeline anterior). */
    val fullText: String,
    val imageWidth: Int,
    val imageHeight: Int
)

/**
 * Uma palavra ou token de texto com a sua posição na imagem.
 *
 * @param text       Texto reconhecido pelo OCR.
 * @param left       Posição esquerda normalizada (0.0 = borda esquerda, 1.0 = borda direita).
 * @param top        Posição superior normalizada (0.0 = topo, 1.0 = fundo).
 * @param right      Posição direita normalizada.
 * @param bottom     Posição inferior normalizada.
 * @param confidence Confiança do OCR (0.0–1.0); 1.0 quando não disponível.
 */
data class OcrElement(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float = 1f
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val height: Float get() = bottom - top
    val width: Float get() = right - left
}
