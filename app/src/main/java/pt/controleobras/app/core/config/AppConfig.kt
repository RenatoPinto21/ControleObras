package pt.controleobras.app.core.config

/**
 * Dados de configuração carregados do ficheiro CONTROLE_OBRAS_CONFIG.xml.
 * As credenciais já chegam aqui desencriptadas — nunca expor em logs.
 */
data class AppConfig(
    val zeid: String,
    val versao: String,
    val empresaNome: String,
    val empresaNif: String,
    val servidor: String,
    val login: String,
    val password: String,
    val porta: String,
    val baseDados: String,
    val modoOnline: Boolean
)
