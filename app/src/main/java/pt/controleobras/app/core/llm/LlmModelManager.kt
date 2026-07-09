package pt.controleobras.app.core.llm

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gere a localização e o estado do ficheiro do modelo LLM no dispositivo.
 *
 * Modelo: Gemma 3 1B IT INT4 (Google AI Edge / LiteRT Community)
 *   - Formato: .task (novo formato MediaPipe 0.10.27+)
 *   - Tamanho: ~529 MB
 *   - Corre no CPU/GPU do tablet via MediaPipe tasks-genai
 *   - Totalmente offline após instalação manual
 *
 * INSTALAÇÃO MANUAL (download requer conta HuggingFace com licença Gemma aceite):
 *   1. Aceitar licença em: https://huggingface.co/litert-community/Gemma3-1B-IT
 *   2. Descarregar: gemma3-1b-it-int4.task (~529 MB)
 *   3. Copiar para o tablet:
 *      adb push gemma3-1b-it-int4.task /sdcard/Android/data/pt.controleobras.app/files/llm/
 *
 * Localização no dispositivo:
 *   Android/data/pt.controleobras.app/files/llm/gemma3-1b-it-int4.task
 */
@Singleton
class LlmModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Nome do ficheiro do modelo (formato .task — MediaPipe 0.10.27+). */
        const val MODEL_FILENAME = "gemma3-1b-it-int4.task"

        /**
         * URL de download do modelo (requer conta HuggingFace + licença Gemma aceite).
         * Usado para mostrar ao administrador onde descarregar o ficheiro.
         */
        const val MODEL_URL =
            "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task"

        /** Texto legível para mostrar na UI. */
        const val MODEL_SIZE_DISPLAY = "~529 MB"

        /**
         * Tamanho mínimo em bytes para considerar o ficheiro válido.
         * O ficheiro real tem ~529 MB — usamos 400 MB como margem de segurança.
         */
        private const val MIN_VALID_SIZE_BYTES = 400_000_000L  // 400 MB
    }

    /** Diretório onde o modelo é guardado. */
    val modelDir: File
        get() = context.getExternalFilesDir("llm")
            ?: File(context.filesDir, "llm")

    /** Caminho completo do ficheiro do modelo. */
    val modelFile: File
        get() = File(modelDir, MODEL_FILENAME)

    /** Caminho como String — passado ao MediaPipe. */
    val modelPath: String
        get() = modelFile.absolutePath

    /**
     * True se o ficheiro existe e tem tamanho suficiente para ser válido.
     * Não faz inferência — apenas verifica o sistema de ficheiros.
     */
    fun modelExists(): Boolean =
        modelFile.exists() && modelFile.length() >= MIN_VALID_SIZE_BYTES

    /** Tamanho atual do ficheiro em MB (0 se não existir). */
    fun modelSizeMb(): Long =
        if (modelFile.exists()) modelFile.length() / 1_000_000L else 0L

    /** Caminho legível para mostrar na UI de configuração. */
    fun modelDirLegivel(): String =
        "Android/data/pt.controleobras.app/files/llm/"
}
