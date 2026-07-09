package pt.controleobras.app.core.parser

import pt.controleobras.app.core.model.ItemTalaoDraft
import pt.controleobras.app.core.model.TalaoDraft
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * Parser heurístico para faturas portuguesas.
 *
 * Estratégia em duas camadas para cada campo:
 *   1. LABEL-FIRST — procura o rótulo explícito ("NIF:", "DATA:", "TOTAL:", etc.)
 *      na mesma linha ou na linha seguinte. Muito fiável quando o campo está rotulado.
 *   2. PATTERN FALLBACK — expressão regular sobre todo o texto para os casos em que
 *      o rótulo não existe ou o OCR o deformou.
 *
 * Cobre os principais tipos de documento emitidos em Portugal:
 *   FT (Fatura), FS (Fatura Simplificada), FR (Fatura-Recibo), NC (Nota de Crédito),
 *   ND (Nota de Débito), talões de caixa (supermercado, restaurante, posto de combustível),
 *   recibos de serviços, faturas de obras/materiais.
 */
class HeuristicReceiptParser @Inject constructor() : ReceiptParser {

    override fun parse(textoReconhecido: String, imagemPath: String): TalaoDraft {
        val linhas = textoReconhecido.lines().map { it.trim() }.filter { it.isNotEmpty() }

        return TalaoDraft(
            empresa      = extrairEmpresa(linhas),
            nif          = extrairNif(textoReconhecido, linhas),
            morada       = extrairMorada(linhas),
            data         = extrairData(textoReconhecido, linhas),
            hora         = extrairHora(textoReconhecido, linhas),
            numeroFatura = extrairNumeroFatura(textoReconhecido, linhas),
            itens        = extrairItens(linhas),
            iva          = extrairIva(textoReconhecido, linhas),
            total        = extrairTotal(textoReconhecido, linhas),
            observacoes  = extrairObservacoes(linhas),
            imagemPath   = imagemPath,
            textoReconhecido = textoReconhecido
        )
    }

    // ───────────────────────────────────────────────────────────────────────────
    // EMPRESA
    // ───────────────────────────────────────────────────────────────────────────

    private fun extrairEmpresa(linhas: List<String>): String {
        val indexLimite = determinarFimCabecalho(linhas).coerceAtLeast(4)
        val cabecalho = linhas.take(indexLimite)

        // Prioridade 1: linha com sufixo legal (Lda., S.A., Unipessoal, etc.)
        cabecalho.firstOrNull { SUFIXO_LEGAL_REGEX.containsMatchIn(it) }
            ?.takeIf { proporcaoLetras(it) >= 0.5 }
            ?.let { return it.trim() }

        // Prioridade 2: linha com rótulo "EMPRESA:", "RAZÃO SOCIAL:", "NOME:"
        cabecalho.firstOrNull { ROTULO_EMPRESA_REGEX.containsMatchIn(it) }
            ?.let { ROTULO_EMPRESA_REGEX.find(it)?.groupValues?.getOrNull(1)?.trim() }
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        // Prioridade 3: scoring — linha maioritariamente alfabética, sem ruído
        val candidatas = cabecalho
            .filter { linha ->
                linha.length >= 3 &&
                    !linhaEhRuido(linha) &&
                    !MORADA_REGEX.containsMatchIn(linha) &&
                    !CODIGO_POSTAL_REGEX.containsMatchIn(linha) &&
                    proporcaoLetras(linha) >= 0.6
            }
            .sortedByDescending { linha ->
                val maiusculas = linha.count { it.isUpperCase() }.toDouble()
                val letras    = linha.count { it.isLetter() }.toDouble().coerceAtLeast(1.0)
                (maiusculas / letras) * linha.length
            }

        return candidatas.firstOrNull().orEmpty()
    }

    private fun proporcaoLetras(linha: String): Double {
        val letras = linha.count { it.isLetter() }
        return letras.toDouble() / linha.length.toDouble().coerceAtLeast(1.0)
    }

    // ───────────────────────────────────────────────────────────────────────────
    // NIF
    // ───────────────────────────────────────────────────────────────────────────

    private fun extrairNif(texto: String, linhas: List<String>): String {
        // L1: rótulo explícito (NIF, NIPC, Contribuinte, NIF/Contribuinte, NUIT)
        NIF_COM_ROTULO_REGEX.find(texto)?.groupValues?.getOrNull(1)
            ?.let { if (nifValido(it)) return it }

        // L2: número de 9 dígitos válido por checksum
        NIF_ISOLADO_REGEX.findAll(texto)
            .map { it.value }
            .firstOrNull { nifValido(it) }
            ?.let { return it }

        return ""
    }

    private fun nifValido(nif: String): Boolean {
        if (nif.length != 9 || !nif.all(Char::isDigit)) return false
        val d = nif.map { it - '0' }
        val soma = d.take(8).mapIndexed { i, v -> v * (9 - i) }.sum()
        val resto = soma % 11
        val ctrl = if (resto < 2) 0 else 11 - resto
        return ctrl == d[8]
    }

    // ───────────────────────────────────────────────────────────────────────────
    // MORADA
    // ───────────────────────────────────────────────────────────────────────────

    private fun extrairMorada(linhas: List<String>): String {
        // Tentar construir morada completa: Rua + Código Postal + Localidade
        val rua = linhas.firstOrNull { VIA_PUBLICA_REGEX.containsMatchIn(it) }
        val cp  = linhas.firstOrNull { CODIGO_POSTAL_REGEX.containsMatchIn(it) }

        return when {
            rua != null && cp != null && rua != cp -> "${rua.trim()}, ${cp.trim()}"
            cp != null -> cp.trim()
            rua != null -> rua.trim()
            else -> ""
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // DATA
    // ───────────────────────────────────────────────────────────────────────────

    private fun extrairData(texto: String, linhas: List<String>): LocalDate? {
        // L1: linha com rótulo DATA / DATE / Dt / Emissão
        for (linha in linhas) {
            if (!ROTULO_DATA_REGEX.containsMatchIn(linha)) continue
            parsarData(linha)?.let { return it }
        }
        // L2: mês por extenso ("15 de Janeiro de 2025")
        DATA_TEXTO_REGEX.find(texto)?.let { m ->
            val dia = m.groupValues[1].toIntOrNull() ?: return@let
            val mes = MES_PT[m.groupValues[2].uppercase()] ?: return@let
            val ano = normalizarAno(m.groupValues[3])
            runCatching { LocalDate.of(ano, mes, dia) }.getOrNull()?.let { return it }
        }
        // L3: padrão numérico dd/mm/aaaa ou aaaa-mm-dd
        parsarData(texto)?.let { return it }
        return null
    }

    private fun parsarData(texto: String): LocalDate? {
        DATA_REGEX.find(texto)?.let { m ->
            val d = m.groupValues[1].toIntOrNull()
            val mo = m.groupValues[2].toIntOrNull()
            val a = normalizarAno(m.groupValues[3])
            if (d != null && mo != null)
                return runCatching { LocalDate.of(a, mo, d) }.getOrNull()
        }
        DATA_ISO_REGEX.find(texto)?.let { m ->
            val a  = m.groupValues[1].toIntOrNull()
            val mo = m.groupValues[2].toIntOrNull()
            val d  = m.groupValues[3].toIntOrNull()
            if (a != null && mo != null && d != null)
                return runCatching { LocalDate.of(a, mo, d) }.getOrNull()
        }
        return null
    }

    private fun normalizarAno(s: String): Int {
        val v = s.toInt()
        return if (s.length == 4) v else if (v <= 79) 2000 + v else 1900 + v
    }

    // ───────────────────────────────────────────────────────────────────────────
    // HORA
    // ───────────────────────────────────────────────────────────────────────────

    private fun extrairHora(texto: String, linhas: List<String>): LocalTime? {
        // L1: linha com rótulo HORA / HOUR / HR / TIME
        for (linha in linhas) {
            if (!ROTULO_HORA_REGEX.containsMatchIn(linha)) continue
            parsarHora(linha)?.let { return it }
        }
        // L2: padrão HH:MM ou HH:MM:SS em qualquer parte do texto
        return parsarHora(texto)
    }

    private fun parsarHora(texto: String): LocalTime? {
        val m = HORA_REGEX.find(texto) ?: return null
        return runCatching {
            val h  = m.groupValues[1].padStart(2, '0')
            val mi = m.groupValues[2]
            val s  = m.groupValues[3].ifEmpty { "00" }
            LocalTime.parse("$h:$mi:$s")
        }.getOrNull()
    }

    // ───────────────────────────────────────────────────────────────────────────
    // NÚMERO DE FATURA
    // ───────────────────────────────────────────────────────────────────────────

    private fun extrairNumeroFatura(texto: String, linhas: List<String>): String {
        // L1: tipo de documento AT seguido de série/número (ex: "FT 2024A/123")
        NUMERO_FATURA_AT_REGEX.find(texto)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.isNotBlank() }?.let { return it }

        // L2: rótulo genérico "Fatura Nº", "Recibo Nº", "Doc. Nº", "Nº:"
        NUMERO_FATURA_ROTULO_REGEX.find(texto)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.isNotBlank() }?.let { return it }

        // L3: padrão livre de série/número (ex: "2024/0001", "A/0001")
        NUMERO_SERIE_REGEX.find(texto)?.value?.trim()
            ?.takeIf { it.isNotBlank() }?.let { return it }

        return ""
    }

    // ───────────────────────────────────────────────────────────────────────────
    // TOTAL
    // ───────────────────────────────────────────────────────────────────────────

    private fun extrairTotal(texto: String, linhas: List<String>): String {
        // L1: "TOTAL A PAGAR", "MONTANTE A PAGAR", "A PAGAR" — mais específico
        TOTAL_A_PAGAR_REGEX.findAll(texto).lastOrNull()
            ?.groupValues?.getOrNull(1)?.replace(',', '.')
            ?.takeIf { it.isNotBlank() }?.let { return it }

        // L2: "TOTAL C/IVA", "TOTAL INCL. IVA", "TOTAL COM IVA"
        TOTAL_COM_IVA_REGEX.findAll(texto).lastOrNull()
            ?.groupValues?.getOrNull(1)?.replace(',', '.')
            ?.takeIf { it.isNotBlank() }?.let { return it }

        // L3: linha com label TOTAL na mesma linha que valor
        for (linha in linhas.reversed()) {
            if (!ROTULO_TOTAL_SIMPLES.containsMatchIn(linha)) continue
            VALOR_MONETARIO_REGEX.findAll(linha).lastOrNull()?.value
                ?.replace(',', '.')?.let { return it }
        }

        // L4: linha com "SOMA", "MONTANTE", "VALOR TOTAL"
        TOTAL_ALTERNATIVO_REGEX.findAll(texto).lastOrNull()
            ?.groupValues?.getOrNull(1)?.replace(',', '.')
            ?.takeIf { it.isNotBlank() }?.let { return it }

        return ""
    }

    // ───────────────────────────────────────────────────────────────────────────
    // IVA
    // ───────────────────────────────────────────────────────────────────────────

    private fun extrairIva(texto: String, linhas: List<String>): String {
        // L1: linha com rótulo "IVA TOTAL", "TOTAL IVA", "IVA INCL."
        IVA_TOTAL_REGEX.find(texto)?.groupValues?.getOrNull(1)
            ?.replace(',', '.')?.takeIf { it.isNotBlank() }?.let { return it }

        // L2: Somar linhas de IVA por taxa (6%, 13%, 23%)
        val valoresIva = IVA_LINHA_REGEX.findAll(texto)
            .mapNotNull { it.groupValues.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull() }
            .toList()
        if (valoresIva.isNotEmpty()) {
            val soma = valoresIva.sum()
            return "%.2f".format(soma)
        }

        // L3: padrão genérico IVA + valor
        IVA_GENERICO_REGEX.find(texto)?.groupValues?.getOrNull(1)
            ?.replace(',', '.')?.takeIf { it.isNotBlank() }?.let { return it }

        return ""
    }

    // ───────────────────────────────────────────────────────────────────────────
    // PRODUTOS / ITENS
    // ───────────────────────────────────────────────────────────────────────────

    private fun extrairItens(linhas: List<String>): List<ItemTalaoDraft> {
        // Identificar zona de produtos: entre o cabeçalho e o rodapé
        val inicio = linhas.indexOfFirst { INICIO_PRODUTOS_REGEX.containsMatchIn(it) }
            .takeIf { it >= 0 } ?: 0
        val fim = linhas.indexOfLast { INICIO_RODAPE_REGEX.containsMatchIn(it) }
            .takeIf { it > inicio } ?: linhas.size

        return linhas.subList(inicio, fim)
            .filter { !linhaEhRuido(it) }
            .mapNotNull { interpretarLinhaProduto(it) }
    }

    private fun interpretarLinhaProduto(linha: String): ItemTalaoDraft? {
        val valores = VALOR_MONETARIO_REGEX.findAll(linha).toList()
        if (valores.isEmpty()) return null

        val total        = valores.last().value.replace(',', '.')
        val precoUnit    = if (valores.size >= 2) valores[valores.size - 2].value.replace(',', '.') else total
        val descBruta    = linha.substring(0, valores.first().range.first).trim()

        // Descrição demasiado curta → provável linha de totais, ignorar
        if (descBruta.length < 2) return null

        // Extrair quantidade do início (ex: "3 x", "2×", "3 UN", "3,000 KG")
        val qMatch    = QUANTIDADE_PREFIXO_REGEX.find(descBruta)
        val quantidade = qMatch?.groupValues?.getOrNull(1)?.trim() ?: "1"
        val descricao  = qMatch
            ?.let { descBruta.substring(it.range.last + 1).trim() }
            ?.ifBlank { descBruta }
            ?: descBruta

        return ItemTalaoDraft(
            descricao    = descricao,
            quantidade   = quantidade,
            precoUnitario = precoUnit,
            total        = total
        )
    }

    // ───────────────────────────────────────────────────────────────────────────
    // OBSERVAÇÕES
    // ───────────────────────────────────────────────────────────────────────────

    private fun extrairObservacoes(linhas: List<String>): String {
        // Linha com "OBS", "OBSERVAÇÕES", "NOTAS", "NOTA:"
        return linhas.firstOrNull { ROTULO_OBS_REGEX.containsMatchIn(it) }
            ?.let { ROTULO_OBS_REGEX.find(it)?.groupValues?.getOrNull(1)?.trim() }
            .orEmpty()
    }

    // ───────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ───────────────────────────────────────────────────────────────────────────

    private fun determinarFimCabecalho(linhas: List<String>): Int {
        for (i in linhas.indices) {
            val l = linhas[i]
            if (DATA_REGEX.containsMatchIn(l) || DATA_ISO_REGEX.containsMatchIn(l)) return i
            if (NIF_COM_ROTULO_REGEX.containsMatchIn(l)) return i
            if (NUMERO_FATURA_AT_REGEX.containsMatchIn(l)) return i
        }
        return linhas.size.coerceAtMost(10)
    }

    private fun linhaEhRuido(linha: String): Boolean {
        val up = linha.uppercase()
        return PALAVRAS_RUIDO.any { up.contains(it) } ||
            linha.length < 2 ||
            proporcaoLetras(linha) < 0.2
    }

    // ───────────────────────────────────────────────────────────────────────────
    // EXPRESSÕES REGULARES
    // ───────────────────────────────────────────────────────────────────────────

    private companion object {

        // Morada
        val CODIGO_POSTAL_REGEX = Regex("""\b\d{4}-\d{3}\b""")
        val VIA_PUBLICA_REGEX = Regex(
            """(?i)\b(rua|r\.|av\.?|avenida|largo|travessa|praceta|estrada|urb\.?|urbaniza[çc][ãa]o|bairro|quinta|lugar|alameda|jardim|parque|bloco|lote|apartamento|apt\.?)\b"""
        )
        val MORADA_REGEX = Regex(
            """(?i)\b(rua|av\.?|avenida|largo|travessa|praceta|estrada)\b|\d{4}-\d{3}"""
        )

        // Empresa
        val SUFIXO_LEGAL_REGEX = Regex(
            """(?i)\b(lda\.?|s\.?a\.?|s\.?a\.?s\.?|unipessoal|sgps|eireli|ltda|s\.?r\.?l\.?|e\.?i\.?r\.?l\.?)\b"""
        )
        val ROTULO_EMPRESA_REGEX = Regex(
            """(?i)(?:EMPRESA|RAZ[ÃA]O SOCIAL|NOME COMERCIAL|EMITENTE)[:\s]+(.+)"""
        )

        // NIF
        val NIF_COM_ROTULO_REGEX = Regex(
            """(?i)(?:NIF|NIPC|Contribuinte|N\.?I\.?F\.?|N\.?I\.?P\.?C\.?)[:\s/]*(\d{9})"""
        )
        val NIF_ISOLADO_REGEX = Regex("""\b\d{9}\b""")

        // Data
        val ROTULO_DATA_REGEX = Regex(
            """(?i)\b(?:DATA|DATE|DT|EMISS[ÃA]O|EMITIDO\s*EM|PROCESSADO\s*EM)[:\s]"""
        )
        val DATA_REGEX     = Regex("""\b(\d{1,2})[/.\-](\d{1,2})[/.\-](\d{2,4})\b""")
        val DATA_ISO_REGEX = Regex("""\b(\d{4})[/.\-](\d{1,2})[/.\-](\d{1,2})\b""")
        val DATA_TEXTO_REGEX = Regex(
            """(?i)(\d{1,2})\s+de\s+(janeiro|fevereiro|mar[çc]o|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro)\s+de\s+(\d{4})"""
        )
        val MES_PT = mapOf(
            "JANEIRO" to 1, "FEVEREIRO" to 2, "MARÇO" to 3, "MARCO" to 3,
            "ABRIL" to 4, "MAIO" to 5, "JUNHO" to 6, "JULHO" to 7,
            "AGOSTO" to 8, "SETEMBRO" to 9, "OUTUBRO" to 10, "NOVEMBRO" to 11, "DEZEMBRO" to 12
        )

        // Hora
        val ROTULO_HORA_REGEX = Regex(
            """(?i)\b(?:HORA|HR|HOUR|TIME|H\.)[:\s]"""
        )
        val HORA_REGEX = Regex("""\b([01]?\d|2[0-3])[:.h]([0-5]\d)(?:[:.]([0-5]\d))?\b""")

        // Número de fatura
        val NUMERO_FATURA_AT_REGEX = Regex(
            """(?i)\b((?:FT|FS|FR|FA|FD|GT|NC|ND|TD|DC|RP|RE|CS|LD|RA)\s+[A-Z0-9]+[/\-]\d+)\b"""
        )
        val NUMERO_FATURA_ROTULO_REGEX = Regex(
            """(?i)(?:FATURA|FAT|RECIBO|REC|DOCUMENTO|DOC|TALÃO|N[ÚU]MERO)\.?\s*N[ºo°\.:]?\s*:?\s*([A-Z0-9/\-]{3,20})"""
        )
        val NUMERO_SERIE_REGEX = Regex("""\b[A-Z]{1,3}[/\-]\d{4,8}\b""")

        // Total
        val TOTAL_A_PAGAR_REGEX = Regex(
            """(?i)(?:TOTAL\s*A\s*PAGAR|MONTANTE\s*A\s*PAGAR|VALOR\s*A\s*PAGAR|A\s*PAGAR)[:\s€]*(\d+[.,]\d{2})"""
        )
        val TOTAL_COM_IVA_REGEX = Regex(
            """(?i)(?:TOTAL\s*(?:C[/.]?\s*IVA|INCL\.?\s*IVA|COM\s*IVA|IVA\s*INCL\.?|GERAL|EUR)|SOMA\s*TOTAL)[:\s€]*(\d+[.,]\d{2})"""
        )
        val ROTULO_TOTAL_SIMPLES = Regex("""(?i)\bTOTAL\b""")
        val TOTAL_ALTERNATIVO_REGEX = Regex(
            """(?i)(?:SOMA|MONTANTE|VALOR\s*TOTAL|TOTAL\s*FATURA)[:\s€]*(\d+[.,]\d{2})"""
        )

        // IVA
        val IVA_TOTAL_REGEX = Regex(
            """(?i)(?:TOTAL\s*IVA|IVA\s*TOTAL|IVA\s*INCLU[ÍI]DO|IVA\s*A\s*PAGAR)[:\s€]*(\d+[.,]\d{2})"""
        )
        val IVA_LINHA_REGEX = Regex(
            """(?i)IVA\s*(?:\d{1,2}\s*%)[:\s€]*(\d+[.,]\d{2})"""
        )
        val IVA_GENERICO_REGEX = Regex(
            """(?i)\bIVA\b[:\s€]*(\d+[.,]\d{2})"""
        )

        // Produtos
        val VALOR_MONETARIO_REGEX  = Regex("""\d+[.,]\d{2}""")
        val QUANTIDADE_PREFIXO_REGEX = Regex(
            """^(\d{1,4}(?:[.,]\d{1,3})?)\s*(?:[xX×*]|un\.?|kg\.?|lt\.?|ml\.?)\s+"""
        )
        val INICIO_PRODUTOS_REGEX  = Regex("""(?i)\b(ARTIGO|PRODUTO|DESCRIÇÃO|ITEM|CÓDIGO)\b""")
        val INICIO_RODAPE_REGEX    = Regex("""(?i)\b(SUBTOTAL|TOTAL|DESCONTO|IVA\s*\d|TROCO|PAGO)\b""")

        // Observações
        val ROTULO_OBS_REGEX = Regex(
            """(?i)(?:OBS\.?|OBSERVA[ÇC][ÕO]ES?|NOTAS?)[:\s]+(.+)"""
        )

        // Palavras que marcam linhas de ruído (a excluir de produtos e empresa)
        val PALAVRAS_RUIDO = listOf(
            "TOTAL", "SUBTOTAL", "IVA", "TROCO", "PAGO", "CONTRIBUINTE", "NIF", "NIPC",
            "OBRIGADO", "CAIXA", "OPERADOR", "MULTIBANCO", "MB WAY", "VISA", "MASTERCARD",
            "ATM", "DESCONTO", "TAXA", "IMPOSTO", "PAGAMENTO", "NUMERADOR", "EMITIDO",
            "TEL", "TELEFONE", "FAX", "WWW.", "HTTP", "EMAIL", "@", "CERTIFICADO",
            "PROCESSADO", "SISTEMA", "SOFTWARE", "PROGRAMA", "TALÃO", "DUPLICADO",
            "ORIGINAL", "CÓPIA", "VIA DO CLIENTE", "VIA DO COMERCIANTE",
            "DOCUMENTO PROCESSADO", "VALIDADE", "AUTORIZA"
        )
    }
}
