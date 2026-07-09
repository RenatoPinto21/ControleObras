package pt.controleobras.app.core.llm

/**
 * Estado atual do download do modelo LLM.
 * Exposto pelo HomeViewModel e consumido pela HomeScreen.
 */
data class LlmDownloadProgress(
    val estado: LlmDownloadEstado = LlmDownloadEstado.IDLE,
    /** 0–100, -1 quando o servidor não informa o tamanho total. */
    val percentagem: Int = 0,
    val bytesDescarregados: Long = 0L,
    val bytesTotal: Long = 0L,
    /** Mensagem de diagnóstico em caso de ERRO (ex: "Código de erro: 1008"). */
    val mensagemErro: String = ""
) {
    /** Ex: "487 MB / 1.3 GB" */
    val descricaoTamanho: String
        get() {
            val descarregado = formatarMb(bytesDescarregados)
            return if (bytesTotal > 0) "$descarregado / ${formatarMb(bytesTotal)}"
            else descarregado
        }

    private fun formatarMb(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000L -> "%.1f GB".format(bytes / 1_000_000_000.0)
            bytes >= 1_000_000L     -> "${bytes / 1_000_000} MB"
            else                    -> "${bytes / 1_000} KB"
        }
    }
}

enum class LlmDownloadEstado {
    /** Sem download em curso. */
    IDLE,
    /** Download em progresso. */
    A_DESCARREGAR,
    /** Download concluído com sucesso. */
    CONCLUIDO,
    /** Erro durante o download. */
    ERRO
}
