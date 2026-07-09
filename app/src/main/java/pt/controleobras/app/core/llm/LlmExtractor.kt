package pt.controleobras.app.core.llm

/**
 * Abstração sobre o modelo de linguagem local (LLM) usado para extrair campos
 * estruturados a partir do texto OCR de uma fatura.
 *
 * Implementação por omissão: MediaPipeLlmExtractor (Gemma 3 1B INT4).
 * Quando o modelo não está instalado, [isModelReady] devolve false e [extract]
 * devolve null — o ViewModel usa então o HeuristicReceiptParser como fallback.
 */
interface LlmExtractor {

    /**
     * True se o ficheiro do modelo está presente no dispositivo e é válido.
     * Pode ser chamado na thread principal (não faz IO pesado).
     */
    fun isModelReady(): Boolean

    /**
     * Analisa [ocrText] com o LLM e devolve os campos estruturados da fatura,
     * ou null em caso de erro / modelo não disponível.
     *
     * Corre num dispatcher de background — nunca chamar na main thread.
     */
    suspend fun extract(ocrText: String): LlmExtractionResult?
}
