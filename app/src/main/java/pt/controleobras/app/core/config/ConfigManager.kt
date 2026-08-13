package pt.controleobras.app.core.config

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lê o ficheiro CONTROLE_OBRAS_CONFIG.xml dos assets da app,
 * desencripta as credenciais (AES-256-CBC) e expõe [AppConfig].
 *
 * A chave e o algoritmo são os mesmos da app Zebra anterior (ConfigManager.cs).
 * Algoritmo: AES-256-CBC | Chave: 32 bytes UTF-8 | IV: 16 bytes a zero
 */
@Singleton
class ConfigManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ConfigManager"
        private const val FICHEIRO = "CONTROLE_OBRAS_CONFIG.xml"

        /**
         * Máscara XOR aplicada aos bytes da chave — evita que a chave
         * apareça como string literal legível no APK descompilado.
         * A chave original é reconstruída em runtime via XOR inverso.
         */
        private const val MASCARA: Int = 0x5A

        /** Bytes da chave AES ofuscados (cada byte XOR com [MASCARA]). */
        private val CHAVE_OFUSCADA = byteArrayOf(
            57, 50, 59, 44, 63, 30, 63, 105,
            104, 24, 35, 46, 63, 41, 107, 98,
            104, 105, 107, 108, 111, 110, 98, 105,
            104, 111, 99, 108, 109, 110, 106, 111
        )

        /** Reconstrói a chave AES original em runtime. */
        private val CHAVE_AES: ByteArray
            get() = CHAVE_OFUSCADA.map { (it.toInt() xor MASCARA).toByte() }.toByteArray()

        /**
         * IV fixo a 16 zeros — mantido por compatibilidade com o sistema C# legado.
         * NOTA: IV fixo enfraquece o CBC mas é exigido pela encriptação existente.
         */
        private val IV_AES = ByteArray(16)
    }

    /** Config carregada — null se o ficheiro não existir ou falhar o parse. */
    @Volatile
    private var configCache: AppConfig? = null

    /**
     * Devolve a configuração (com cache).
     * Lança [IllegalStateException] se o ficheiro não existir nos assets.
     */
    fun obterConfig(): AppConfig {
        return configCache ?: carregar().also { configCache = it }
    }

    private fun carregar(): AppConfig {
        val stream: InputStream = try {
            context.assets.open(FICHEIRO)
        } catch (e: Exception) {
            Log.e(TAG, "Ficheiro $FICHEIRO não encontrado nos assets", e)
            throw IllegalStateException("Ficheiro de configuração ausente: $FICHEIRO", e)
        }

        return try {
            val doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(stream)

            doc.documentElement.normalize()
            val raiz = doc.documentElement

            val empresa = raiz.primeiroElemento("EMPRESA")
            val bd      = empresa.primeiroElemento("BD")
            val tipo    = empresa.primeiroElemento("TIPOLIGACAO")

            AppConfig(
                zeid       = raiz.lerTexto("ZEID"),
                versao     = raiz.lerTexto("VERSAO"),
                empresaNome = empresa.lerTexto("Id"),
                empresaNif  = empresa.lerTexto("NIF"),
                servidor   = desencriptar(bd.lerTexto("SERVIDOR"),  "SERVIDOR"),
                login      = desencriptar(bd.lerTexto("LOGIN"),     "LOGIN"),
                password   = desencriptar(bd.lerTexto("PASSWORD"),  "PASSWORD"),
                porta      = desencriptar(bd.lerTexto("PORTA"),     "PORTA"),
                baseDados  = desencriptar(bd.lerTexto("BASEDADOS"), "BASEDADOS"),
                modoOnline = tipo.lerTexto("ONLINE") == "1"
            ).also {
                Log.d(TAG, "Config carregada com sucesso")
            }
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao fazer parse do XML de configuração", e)
            throw IllegalStateException("Erro ao ler configuração: ${e.message}", e)
        }
    }

    // ── Extensões de parse ────────────────────────────────────────────────────

    private fun Element.lerTexto(tag: String): String {
        val lista = getElementsByTagName(tag)
        return if (lista.length > 0) lista.item(0).textContent.trim() else ""
    }

    private fun Element.primeiroElemento(tag: String): Element {
        val lista = getElementsByTagName(tag)
        if (lista.length == 0) throw IllegalStateException("Elemento <$tag> não encontrado no XML")
        var i = 0
        while (i < lista.length) {
            val no = lista.item(i)
            if (no.nodeType == Node.ELEMENT_NODE) return no as Element
            i++
        }
        throw IllegalStateException("Elemento <$tag> não encontrado no XML")
    }

    // ── Desencriptação AES-256-CBC ────────────────────────────────────────────

    private fun desencriptar(base64: String, campo: String): String {
        if (base64.isBlank()) return ""
        return try {
            val bytes  = Base64.getDecoder().decode(base64)
            val chave  = SecretKeySpec(CHAVE_AES, "AES")
            val iv     = IvParameterSpec(IV_AES)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, chave, iv)
            String(cipher.doFinal(bytes), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao desencriptar $campo: ${e.message}")
            ""
        }
    }
}
