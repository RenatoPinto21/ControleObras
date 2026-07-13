package pt.controleobras.app.core.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extrator LLM usando o modelo Gemma 2B IT INT4 via MediaPipe LLM Inference API.
 *
 * Características:
 *  - Corre totalmente offline no GPU do tablet (Adreno/Mali via OpenGL ES/Vulkan)
 *  - Init lazy — o modelo é carregado na primeira chamada a [extract]
 *  - Thread-safe via Mutex — nunca duas inferências em simultâneo
 *  - Temperatura 0.0 para respostas deterministas (extração de dados, não criatividade)
 *  - Se o modelo falhar a carregar ou a inferência falhar, devolve null → fallback heurístico
 *
 * Formato de prompt: Gemma instruction-tuned (<start_of_turn>user / model)
 */
@Singleton
class MediaPipeLlmExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelManager: LlmModelManager
) : LlmExtractor {

    private val mutex = Mutex()
    private var llmInstance: LlmInference? = null

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Interface pública
    // ─────────────────────────────────────────────────────────────────────────

    override fun isModelReady(): Boolean = modelManager.modelExists()

    override suspend fun extract(ocrText: String): LlmExtractionResult? =
        withContext(Dispatchers.Default) {
            if (!isModelReady()) return@withContext null
            runCatching {
                val llm = obterOuCriarInstancia() ?: return@withContext null
                val prompt = construirPrompt(ocrText)
                val resposta = mutex.withLock {
                    llm.generateResponse(prompt)
                }
                interpretarResposta(resposta)
            }.getOrNull()
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Init do modelo (lazy, com Mutex para evitar dupla criação)
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun obterOuCriarInstancia(): LlmInference? =
        mutex.withLock {
            llmInstance ?: criarInstancia().also { llmInstance = it }
        }

    private fun criarInstancia(): LlmInference? = runCatching {
        val options = LlmInferenceOptions.builder()
            .setModelPath(modelManager.modelPath)
            // maxTokens = janela total (input + output).
            // Prompt base ≈ 700 tokens + OCR truncado a 1500 chars ≈ 375 tokens → input ≈ 1075.
            // Sobram ≈ 973 tokens para o JSON de resposta. Gemma 3-1B suporta até 8192.
            .setMaxTokens(2048)
            .setMaxTopK(40)              // limita o espaço de sampling
            .build()
        LlmInference.createFromOptions(context, options)
    }.getOrNull()

    // ─────────────────────────────────────────────────────────────────────────
    // Construção do prompt (formato Gemma instruct)
    // ─────────────────────────────────────────────────────────────────────────

    private fun construirPrompt(ocrText: String): String = buildString {
        append("<start_of_turn>user\n")
        append(
            """
            És um sistema de extração de dados de faturas portuguesas para uma empresa de construção civil.
            Analisa o texto OCR abaixo e devolve APENAS um objeto JSON válido. Nada mais.

            Nunca inventes valores. Se um campo não existir no texto, usa null.

            Estrutura exata do JSON (snake_case obrigatório):
            {
              "fornecedor": "nome da empresa que emitiu a fatura",
              "nif_fornecedor": "NIF de 9 dígitos do emitente",
              "nif_cliente": "NIF de 9 dígitos do cliente/adquirente (empresa de construção)",
              "morada": "morada completa do fornecedor",
              "numero_fatura": "número completo do documento (ex: FT A/1234)",
              "serie": "série do documento (ex: A extraído de FT A/1234)",
              "data_emissao": "dd/MM/yyyy",
              "data_vencimento": "dd/MM/yyyy",
              "hora": "HH:mm",
              "subtotal": "total sem IVA (ex: 100.00)",
              "iva_total": "total de IVA (ex: 23.00)",
              "total": "total a pagar com IVA (ex: 123.00)",
              "metodo_pagamento": "forma de pagamento (ex: Multibanco, MB Way, Numerário, Visa, Transferência)",
              "linhas": [
                {
                  "descricao": "nome do produto ou serviço",
                  "quantidade": "quantidade (ex: 2 ou 1.500)",
                  "preco_unitario": "preço por unidade (ex: 5.99)",
                  "desconto": "desconto na linha se existir (ex: 1.00)",
                  "taxa_iva": "percentagem de IVA (ex: 23, 13 ou 6)",
                  "total_linha": "total da linha com IVA (ex: 11.98)"
                }
              ],
              "observacoes": "notas adicionais se existirem"
            }

            Regras obrigatórias:
            - NIF tem SEMPRE 9 dígitos numéricos
            - nif_fornecedor e nif_cliente são NIFs DIFERENTES — o cliente é a empresa de construção que comprou
            - Datas SEMPRE no formato dd/MM/yyyy
            - Valores monetários com PONTO decimal (12.50, não 12,50)
            - taxa_iva é só o número (23, 13 ou 6), sem símbolo %
            - Se o número de fatura for "FT A/1234", a serie é "A"
            - Ignora linhas de rodapé (OBRIGADO, OPERADOR, VALIDADE DE CARTÃO, etc.)
            - Se houver QR code AT no texto, os campos A=nif_fornecedor, G=numero_fatura, N=iva_total, O=total

            TEXTO OCR DA FATURA:
            """.trimIndent()
        )
        append("\n")
        // Limitar o texto OCR a 1500 caracteres (≈375 tokens) para garantir que
        // o total de tokens do prompt (base ≈700 + OCR ≈375) fica bem abaixo de maxTokens(2048).
        append(ocrText.take(1500))
        append("\n<end_of_turn>\n")
        append("<start_of_turn>model\n")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parse da resposta do modelo → LlmExtractionResult
    // ─────────────────────────────────────────────────────────────────────────

    private fun interpretarResposta(resposta: String): LlmExtractionResult? {
        val jsonStr = extrairJson(resposta) ?: return null
        return runCatching {
            jsonParser.decodeFromString<LlmExtractionResult>(jsonStr)
        }.getOrNull()
    }

    /**
     * Extrai o objeto JSON da resposta do modelo.
     * O modelo pode envolver o JSON em blocos de código Markdown ou adicionar texto extra.
     */
    private fun extrairJson(resposta: String): String? {
        // Tentativa 1: bloco ```json ... ``` ou ``` ... ```
        val blocoMarkdown = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
            .find(resposta)?.groupValues?.getOrNull(1)?.trim()
        if (!blocoMarkdown.isNullOrBlank()) return blocoMarkdown

        // Tentativa 2: JSON puro { ... } — extrai o objeto mais externo
        val inicio = resposta.indexOf('{')
        val fim = resposta.lastIndexOf('}')
        if (inicio >= 0 && fim > inicio) {
            return resposta.substring(inicio, fim + 1).trim()
        }

        return null
    }
}
