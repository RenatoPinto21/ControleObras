package pt.controleobras.app.core.extractor

import pt.controleobras.app.core.model.ItemTalaoDraft
import pt.controleobras.app.core.model.TalaoDraft
import pt.controleobras.app.core.ocr.OcrElement
import pt.controleobras.app.core.ocr.StructuredOcrResult
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Extrator de dados de faturas baseado na posição visual dos elementos OCR.
 *
 * Ao contrário do parser heurístico (que trabalha com texto concatenado),
 * este extrator usa as coordenadas (x, y) normalizadas de cada palavra
 * para reconstruir a estrutura visual da fatura:
 *
 *  ┌─────────────────────────────────────┐
 *  │  CABEÇALHO  (Y: 0.00 – 0.40)       │ empresa, NIF, morada, nº fatura, data, hora
 *  ├─────────────────────────────────────┤
 *  │  CORPO      (Y: 0.35 – 0.75)       │ tabela de produtos (linhas × colunas)
 *  ├─────────────────────────────────────┤
 *  │  RODAPÉ     (Y: 0.65 – 1.00)       │ subtotal, IVA, total, método pagamento
 *  └─────────────────────────────────────┘
 *
 * As zonas têm sobreposição intencional para faturas com layouts não-standard.
 */
@Singleton
class PositionAwareReceiptExtractor @Inject constructor() {

    // ─────────────────────────────────────────────────────────────────────────
    // Limites das zonas (coordenadas Y normalizadas)
    // ─────────────────────────────────────────────────────────────────────────

    private val HEADER_TOP    = 0.00f
    private val HEADER_BOTTOM = 0.42f
    private val BODY_TOP      = 0.33f
    private val BODY_BOTTOM   = 0.78f
    private val FOOTER_TOP    = 0.63f
    private val FOOTER_BOTTOM = 1.00f

    // Limiares de agrupamento
    private val ROW_THRESHOLD = 0.018f   // elementos com |centerY diff| < este valor são a mesma linha
    private val COL_DESC_MAX  = 0.52f    // X máximo para "descrição do produto"
    private val COL_QTD_MAX   = 0.66f    // X máximo para "quantidade"
    private val COL_PRECO_MAX = 0.82f    // X máximo para "preço unitário"
    // X >= COL_PRECO_MAX → "total da linha"

    // ─────────────────────────────────────────────────────────────────────────
    // Ponto de entrada
    // ─────────────────────────────────────────────────────────────────────────

    fun extract(ocr: StructuredOcrResult, imagemPath: String): TalaoDraft {
        val cabecalho = ocr.elements.filter { it.centerY in HEADER_TOP..HEADER_BOTTOM }
        val corpo     = ocr.elements.filter { it.centerY in BODY_TOP..BODY_BOTTOM }
        val rodape    = ocr.elements.filter { it.centerY in FOOTER_TOP..FOOTER_BOTTOM }

        val textoCompleto = ocr.fullText

        val draft = TalaoDraft(
            empresa          = extrairEmpresa(cabecalho),
            nif              = extrairNif(cabecalho, textoCompleto, null),
            nifCliente       = extrairNifCliente(cabecalho, textoCompleto),
            morada           = extrairMorada(cabecalho),
            numeroFatura     = extrairNumeroFatura(cabecalho, textoCompleto),
            serie            = extrairSerie(textoCompleto),
            data             = extrairData(cabecalho, textoCompleto),
            hora             = extrairHora(cabecalho, textoCompleto),
            dataVencimento   = null,
            metodoPagamento  = extrairMetodoPagamento(rodape, textoCompleto),
            itens            = extrairItens(corpo),
            iva              = extrairValorComRotulo(rodape, textoCompleto, IVA_ROTULOS),
            total            = extrairTotal(rodape, textoCompleto),
            observacoes      = "",
            imagemPath       = imagemPath,
            textoReconhecido = textoCompleto
        )
        // Filtro de sanidade — anula campos que claramente têm lixo de OCR
        return sanitizar(draft)
    }

    /**
     * Filtro pós-extração: verifica cada campo e anula os que claramente contêm lixo.
     *
     * Regras:
     *  - Empresa:  max 60 chars, pelo menos 35% letras, sem palavras de ruído
     *  - Morada:   validação estrutural completa (comprimento, densidade de dígitos,
     *              densidade de tokens de lixo monetário, ausência de ruído de rodapé)
     *  - NIF:      9 dígitos e checksum módulo 11 válido
     *  - Total/IVA: formato numérico exato X.XX (sem texto à volta)
     */
    private fun sanitizar(draft: TalaoDraft): TalaoDraft {
        return draft.copy(
            empresa = draft.empresa
                .takeIf { it.length in 2..60 && proporcaoLetras(it) >= 0.35 && !EH_RUIDO_MORADA_REGEX.containsMatchIn(it.uppercase()) }
                .orEmpty(),
            morada  = draft.morada.takeIf { moradaValida(it) }.orEmpty(),
            nif        = draft.nif.takeIf { nifValido(it) }.orEmpty(),
            nifCliente = draft.nifCliente.takeIf { nifValido(it) }.orEmpty(),
            iva     = draft.iva.takeIf { VALOR_EXATO_REGEX.matches(it) }.orEmpty(),
            total   = draft.total.takeIf { VALOR_EXATO_REGEX.matches(it) }.orEmpty()
        )
    }

    /**
     * Validação estrutural de morada.
     *
     * Uma morada real tem:
     *  - Entre 5 e 140 caracteres
     *  - Sem palavras de ruído de rodapé (TOTAL, IVA, MB WAY, etc.)
     *  - Densidade de dígitos baixa: os únicos dígitos num endereço são
     *    o número de porta e o código postal — logo < 30% do texto
     *  - No máximo 1 token de lixo monetário (€, %, "2x", "1,500 kg", etc.)
     */
    private fun moradaValida(texto: String): Boolean {
        if (texto.isBlank() || texto.length !in 5..140) return false
        if (EH_RUIDO_MORADA_REGEX.containsMatchIn(texto.uppercase())) return false
        val ratioDigitos = texto.count { it.isDigit() }.toDouble() / texto.length
        if (ratioDigitos > 0.30) return false
        if (LIXO_MONETARIO_REGEX.findAll(texto).count() > 1) return false
        return true
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CABEÇALHO — empresa
    // ─────────────────────────────────────────────────────────────────────────

    private fun extrairEmpresa(elementos: List<OcrElement>): String {
        // Linha de maior altura de caixa no topo (provavelmente o nome da empresa em tamanho maior)
        val linhasOrdenadas = agruparEmLinhas(elementos).sortedBy { it.first().centerY }

        // Primeiro: linha com sufixo legal
        for (linha in linhasOrdenadas.take(8)) {
            val texto = linha.joinToString(" ") { it.text }
            if (SUFIXO_LEGAL_REGEX.containsMatchIn(texto) && texto.length >= 3) return texto.trim()
        }

        // Segundo: linha mais alta (maior altura média dos elementos → fonte maior → nome)
        val maisAlta = linhasOrdenadas.take(5)
            .maxByOrNull { linha -> linha.maxOf { it.height } }
        if (maisAlta != null) {
            val texto = maisAlta.joinToString(" ") { it.text }
            if (texto.length >= 3 && !EH_RUIDO_REGEX.containsMatchIn(texto)) return texto.trim()
        }

        // Terceiro: primeira linha com proporção de letras ≥ 60%
        return linhasOrdenadas.take(6)
            .map { it.joinToString(" ") { e -> e.text } }
            .firstOrNull { t -> t.length >= 3 && proporcaoLetras(t) >= 0.6 && !EH_RUIDO_REGEX.containsMatchIn(t) }
            .orEmpty()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CABEÇALHO — NIF fornecedor
    // ─────────────────────────────────────────────────────────────────────────

    private fun extrairNif(elementos: List<OcrElement>, textoCompleto: String, excluir: String?): String {
        val textoZona = elementos.joinToString(" ") { it.text }
        // L1: rótulo explícito de NIF do fornecedor
        NIF_FORNECEDOR_REGEX.find(textoZona)?.groupValues?.getOrNull(1)
            ?.takeIf { it != excluir && nifValido(it) }?.let { return it }
        NIF_FORNECEDOR_REGEX.find(textoCompleto)?.groupValues?.getOrNull(1)
            ?.takeIf { it != excluir && nifValido(it) }?.let { return it }
        // L2: qualquer NIF válido na zona
        NIF_ISOLADO_REGEX.findAll(textoZona)
            .map { it.value }
            .firstOrNull { it != excluir && nifValido(it) }
            ?.let { return it }
        return ""
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CABEÇALHO — NIF cliente
    // ─────────────────────────────────────────────────────────────────────────

    private fun extrairNifCliente(elementos: List<OcrElement>, textoCompleto: String): String {
        val textoZona = elementos.joinToString(" ") { it.text }
        // L1: rótulo explícito de NIF do cliente/adquirente
        NIF_CLIENTE_REGEX.find(textoZona)?.groupValues?.getOrNull(1)
            ?.takeIf { nifValido(it) }?.let { return it }
        NIF_CLIENTE_REGEX.find(textoCompleto)?.groupValues?.getOrNull(1)
            ?.takeIf { nifValido(it) }?.let { return it }
        // L2: segundo NIF distinto do fornecedor
        val nifFornecedor = extrairNif(elementos, textoCompleto, null)
        return NIF_ISOLADO_REGEX.findAll(textoCompleto)
            .map { it.value }
            .filter { it != nifFornecedor && nifValido(it) }
            .firstOrNull()
            .orEmpty()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CABEÇALHO — morada
    // ─────────────────────────────────────────────────────────────────────────

    private fun extrairMorada(elementos: List<OcrElement>): String {
        val linhas = agruparEmLinhas(elementos).map { it.joinToString(" ") { e -> e.text } }

        // Linha de rua: tem palavra de via pública, ≤ 90 chars, sem ruído de rodapé
        val rua = linhas.firstOrNull { linha ->
            VIA_REGEX.containsMatchIn(linha) &&
            linha.length <= 90 &&
            !EH_RUIDO_MORADA_REGEX.containsMatchIn(linha.uppercase())
        }
        // Linha de código postal: padrão XXXX-XXX, ≤ 60 chars
        val cp = linhas.firstOrNull { linha ->
            CP_REGEX.containsMatchIn(linha) && linha.length <= 60
        }
        return when {
            rua != null && cp != null && rua != cp -> "${rua.trim()}, ${cp.trim()}"
            cp  != null -> cp.trim()
            rua != null -> rua.trim()
            else        -> ""
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CABEÇALHO — número de fatura / série
    // ─────────────────────────────────────────────────────────────────────────

    private fun extrairNumeroFatura(elementos: List<OcrElement>, textoCompleto: String): String {
        val textoZona = elementos.joinToString(" ") { it.text }
        NUMERO_FATURA_AT_REGEX.find(textoZona)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.isNotBlank() }?.let { return it }
        NUMERO_FATURA_AT_REGEX.find(textoCompleto)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.isNotBlank() }?.let { return it }
        NUMERO_FATURA_ROTULO_REGEX.find(textoCompleto)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.isNotBlank() }?.let { return it }
        return ""
    }

    private fun extrairSerie(textoCompleto: String): String {
        return SERIE_REGEX.find(textoCompleto)?.groupValues?.getOrNull(1)?.trim().orEmpty()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CABEÇALHO — data / hora
    // ─────────────────────────────────────────────────────────────────────────

    private fun extrairData(elementos: List<OcrElement>, textoCompleto: String): LocalDate? {
        val textoZona = elementos.joinToString(" ") { it.text }
        return parsarData(textoZona) ?: parsarData(textoCompleto)
    }

    private fun parsarData(texto: String): LocalDate? {
        DATA_REGEX.find(texto)?.let { m ->
            val d  = m.groupValues[1].toIntOrNull() ?: return@let
            val mo = m.groupValues[2].toIntOrNull() ?: return@let
            val a  = normalizarAno(m.groupValues[3])
            return runCatching { LocalDate.of(a, mo, d) }.getOrNull()
        }
        DATA_ISO_REGEX.find(texto)?.let { m ->
            val a  = m.groupValues[1].toIntOrNull() ?: return@let
            val mo = m.groupValues[2].toIntOrNull() ?: return@let
            val d  = m.groupValues[3].toIntOrNull() ?: return@let
            return runCatching { LocalDate.of(a, mo, d) }.getOrNull()
        }
        return null
    }

    private fun extrairHora(elementos: List<OcrElement>, textoCompleto: String): LocalTime? {
        val textoZona = elementos.joinToString(" ") { it.text }
        return parsarHora(textoZona) ?: parsarHora(textoCompleto)
    }

    private fun parsarHora(texto: String): LocalTime? {
        val m = HORA_REGEX.find(texto) ?: return null
        return runCatching {
            LocalTime.parse(
                "${m.groupValues[1].padStart(2, '0')}:${m.groupValues[2]}",
                DateTimeFormatter.ofPattern("HH:mm")
            )
        }.getOrNull()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CORPO — tabela de produtos
    // ─────────────────────────────────────────────────────────────────────────

    private fun extrairItens(elementos: List<OcrElement>): List<ItemTalaoDraft> {
        if (elementos.isEmpty()) return emptyList()

        val linhas = agruparEmLinhas(elementos).sortedBy { it.first().centerY }

        // Filtrar linhas de cabeçalho de tabela e linhas de totais
        val linhasProduto = linhas.filter { linha ->
            val texto = linha.joinToString(" ") { it.text }.uppercase()
            !ROTULO_TABELA_REGEX.containsMatchIn(texto) &&
            !TOTAL_REGEX.containsMatchIn(texto) &&
            !EH_RUIDO_REGEX.containsMatchIn(texto) &&
            temAlgumNumero(texto)
        }

        return linhasProduto.mapNotNull { linha ->
            interpretarLinhaComPosicao(linha)
        }
    }

    private fun interpretarLinhaComPosicao(elementos: List<OcrElement>): ItemTalaoDraft? {
        // Separar por coluna usando posição X
        val descricao  = elementos.filter { it.centerX < COL_DESC_MAX }
            .sortedBy { it.centerX }
            .joinToString(" ") { it.text }
            .trim()

        val quantidade = elementos.filter { it.centerX in COL_DESC_MAX..COL_QTD_MAX }
            .joinToString(" ") { it.text }
            .trim()

        val precoUnit  = elementos.filter { it.centerX in COL_QTD_MAX..COL_PRECO_MAX }
            .joinToString(" ") { it.text }
            .trim()

        val totalLinha = elementos.filter { it.centerX >= COL_PRECO_MAX }
            .joinToString(" ") { it.text }
            .trim()

        // Linha inválida: sem descrição ou sem nenhum valor numérico
        if (descricao.length < 2) return null
        val temValor = VALOR_REGEX.containsMatchIn(totalLinha) ||
                       VALOR_REGEX.containsMatchIn(precoUnit) ||
                       VALOR_REGEX.containsMatchIn(quantidade)
        if (!temValor) return null

        return ItemTalaoDraft(
            descricao     = descricao,
            quantidade    = normalizarNumero(quantidade),
            precoUnitario = normalizarNumero(precoUnit),
            total         = normalizarNumero(totalLinha)
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RODAPÉ — total / IVA / método pagamento
    // ─────────────────────────────────────────────────────────────────────────

    private fun extrairTotal(elementos: List<OcrElement>, textoCompleto: String): String {
        // Procurar na zona do rodapé a linha com "TOTAL A PAGAR" ou "TOTAL" + valor maior
        val linhasRodape = agruparEmLinhas(elementos).sortedByDescending { it.first().centerY }
        for (linha in linhasRodape) {
            val texto = linha.joinToString(" ") { it.text }.uppercase()
            if (TOTAL_PAGAR_REGEX.containsMatchIn(texto)) {
                VALOR_REGEX.findAll(texto).lastOrNull()?.value?.replace(',', '.')
                    ?.let { return it }
            }
        }
        for (linha in linhasRodape) {
            val texto = linha.joinToString(" ") { it.text }.uppercase()
            if (TOTAL_SIMPLES_REGEX.containsMatchIn(texto)) {
                VALOR_REGEX.findAll(texto).lastOrNull()?.value?.replace(',', '.')
                    ?.let { return it }
            }
        }
        // Fallback no texto completo
        TOTAL_COMPLETO_REGEX.find(textoCompleto)?.groupValues?.getOrNull(1)
            ?.replace(',', '.')?.let { return it }
        return ""
    }

    private fun extrairValorComRotulo(
        elementos: List<OcrElement>,
        textoCompleto: String,
        rotulos: Regex
    ): String {
        val textoZona = agruparEmLinhas(elementos)
            .map { it.joinToString(" ") { e -> e.text } }
            .firstOrNull { rotulos.containsMatchIn(it.uppercase()) }

        if (textoZona != null) {
            VALOR_REGEX.findAll(textoZona).lastOrNull()?.value?.replace(',', '.')
                ?.let { return it }
        }
        // Fallback texto completo
        buildValorAposRotuloRegex(rotulos).find(textoCompleto)?.groupValues?.getOrNull(1)
            ?.replace(',', '.')?.let { return it }
        return ""
    }

    private fun extrairMetodoPagamento(elementos: List<OcrElement>, textoCompleto: String): String {
        val textoZona = elementos.joinToString(" ") { it.text }.uppercase()
        val alvo = if (textoZona.isNotBlank()) textoZona else textoCompleto.uppercase()
        return when {
            "MB WAY"       in alvo || "MBWAY"          in alvo -> "MB Way"
            "MULTIBANCO"   in alvo || " ATM"           in alvo -> "Multibanco"
            "TRANSFERÊNCIA" in alvo || "TRANSFERENCIA" in alvo -> "Transferência"
            "VISA"         in alvo && "NUMERÁRIO" !in alvo     -> "Visa"
            "MASTERCARD"   in alvo                             -> "Mastercard"
            "NUMERÁRIO"    in alvo || "NUMERARIO"     in alvo  -> "Numerário"
            "DINHEIRO"     in alvo                             -> "Numerário"
            "CHEQUE"       in alvo                             -> "Cheque"
            else                                               -> ""
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Agrupamento de elementos em linhas por posição Y
    // ─────────────────────────────────────────────────────────────────────────

    private fun agruparEmLinhas(elementos: List<OcrElement>): List<List<OcrElement>> {
        if (elementos.isEmpty()) return emptyList()
        val ordenados = elementos.sortedBy { it.centerY }
        val linhas = mutableListOf<MutableList<OcrElement>>()
        for (elem in ordenados) {
            val linhaExistente = linhas.lastOrNull {
                abs(it.last().centerY - elem.centerY) <= ROW_THRESHOLD
            }
            if (linhaExistente != null) linhaExistente.add(elem)
            else linhas.add(mutableListOf(elem))
        }
        // Ordenar cada linha da esquerda para a direita
        return linhas.map { it.sortedBy { e -> e.centerX } }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun normalizarNumero(s: String): String = s.replace(',', '.').trim()

    private fun proporcaoLetras(s: String): Double {
        val letras = s.count { it.isLetter() }
        return letras.toDouble() / s.length.coerceAtLeast(1).toDouble()
    }

    private fun temAlgumNumero(s: String): Boolean = s.any { it.isDigit() }

    private fun normalizarAno(s: String): Int {
        val v = s.toInt()
        return if (s.length == 4) v else if (v <= 79) 2000 + v else 1900 + v
    }

    private fun nifValido(nif: String): Boolean {
        if (nif.length != 9 || !nif.all(Char::isDigit)) return false
        val d = nif.map { it - '0' }
        if (d[0] == 0 || d[0] == 4) return false
        val soma = d.take(8).mapIndexed { i, v -> v * (9 - i) }.sum()
        val resto = soma % 11
        val ctrl = if (resto < 2) 0 else 11 - resto
        return ctrl == d[8]
    }

    private fun buildValorAposRotuloRegex(rotulo: Regex): Regex =
        Regex("(?i)${rotulo.pattern}[:\\s€]*([\\d]+[.,]\\d{2})")

    // ─────────────────────────────────────────────────────────────────────────
    // Expressões regulares
    // ─────────────────────────────────────────────────────────────────────────

    private companion object {
        val SUFIXO_LEGAL_REGEX = Regex(
            """(?i)\b(lda\.?|s\.?a\.?|unipessoal|sgps|eireli|ltda|s\.?r\.?l\.?)\b"""
        )
        val EH_RUIDO_REGEX = Regex(
            """(?i)\b(TOTAL|SUBTOTAL|IVA|TROCO|PAGO|NIF|NIPC|OBRIGADO|CAIXA|OPERADOR|MULTIBANCO|ATM|DESCONTO|TAXA|IMPOSTO|TEL|FAX|CERTIFICADO|DOCUMENTO PROCESSADO)\b"""
        )
        // Ruído específico para linhas de morada — impede que linhas longas de OCR sejam confundidas com endereços
        val EH_RUIDO_MORADA_REGEX = Regex(
            """(?i)\b(TOTAL|SUBTOTAL|IVA|TROCO|PAGO|DESCONTO|PAGAMENTO|FATURA|RECIBO|CÓPIA|DUPLICADO|OBRIGADO|TALÃO|CARTAO|DEPOSITO|COMPRA|PRODUTO|ARTIGO|PREÇO|QUANTIDADE|TERMINAL|OPERADOR|CAIXA|MULTIBANCO|MB WAY|VISA|MASTERCARD)\b"""
        )
        val NIF_FORNECEDOR_REGEX = Regex(
            """(?i)(?:NIF|NIPC|Contribuinte|N\.?I\.?F\.?)[:\s/]*(\d{9})"""
        )
        val NIF_CLIENTE_REGEX = Regex(
            """(?i)(?:NIF\s*(?:DO\s*)?CLIENTE|CLIENTE[:\s]+NIF|ADQUIRENTE[:\s]*NIF|NIF\s*ADQUIRENTE|NIF\s*COMPRADOR|A/C)[:\s/]*(\d{9})"""
        )
        val NIF_ISOLADO_REGEX = Regex("""\b(\d{9})\b""")
        val VIA_REGEX = Regex(
            """(?i)\b(rua|r\.|av\.?|avenida|largo|travessa|praceta|estrada|urb\.?|bairro|quinta|alameda)\b"""
        )
        val CP_REGEX = Regex("""\b\d{4}-\d{3}\b""")
        val NUMERO_FATURA_AT_REGEX = Regex(
            """(?i)\b((?:FT|FS|FR|FA|NC|ND|GT|DC|RP)\s+[A-Z0-9]+[/\-]\d+)\b"""
        )
        val NUMERO_FATURA_ROTULO_REGEX = Regex(
            """(?i)(?:FATURA|FAT|RECIBO|DOC|N[ÚU]MERO)\.?\s*N[ºo°\.:]?\s*:?\s*([A-Z0-9/\-]{3,20})"""
        )
        val SERIE_REGEX = Regex(
            """(?i)(?:FT|FS|FR|NC|ND)\s+([A-Z0-9]+)/"""
        )
        val DATA_REGEX     = Regex("""\b(\d{1,2})[/.\-](\d{1,2})[/.\-](\d{2,4})\b""")
        val DATA_ISO_REGEX = Regex("""\b(\d{4})[/.\-](\d{1,2})[/.\-](\d{1,2})\b""")
        val HORA_REGEX     = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""")
        val VALOR_REGEX        = Regex("""\d+[.,]\d{2}""")
        val VALOR_EXATO_REGEX  = Regex("""^\d+[.,]\d{2}$""")
        val TOTAL_PAGAR_REGEX  = Regex("""(?i)TOTAL\s*(A\s*PAGAR|COM\s*IVA|C/IVA|INCL\.|GERAL)""")
        val TOTAL_SIMPLES_REGEX = Regex("""(?i)\bTOTAL\b""")
        val TOTAL_COMPLETO_REGEX = Regex(
            """(?i)(?:TOTAL\s*A\s*PAGAR|TOTAL\s*C[/.]?\s*IVA|TOTAL\s*COM\s*IVA|MONTANTE\s*A\s*PAGAR)[:\s€]*(\d+[.,]\d{2})"""
        )
        val ROTULO_TABELA_REGEX = Regex(
            """(?i)\b(ARTIGO|PRODUTO|DESCRIÇÃO|ITEM|CÓDIGO|REFERÊNCIA|QTD|QT\.?|QUANTIDADE|PREÇO|UNITÁRIO|DESCONTO|TAXA)\b"""
        )
        val TOTAL_REGEX = Regex(
            """(?i)\b(TOTAL|SUBTOTAL|IVA|TROCO|PAGO|A\s*PAGAR)\b"""
        )
        val IVA_ROTULOS = Regex("""(?i)\b(IVA|IMPOSTO)\b""")
        // Tokens que nunca aparecem numa morada real: símbolos monetários, percentagens,
        // multiplicadores de quantidade (2x, 3×) e pesos/volumes no formato numérico
        val LIXO_MONETARIO_REGEX = Regex("""[€%]|\b(\d+\s*[xX×]|\d+[,.]\d{3}\s*(kg|lt|un))\b""")
    }
}
