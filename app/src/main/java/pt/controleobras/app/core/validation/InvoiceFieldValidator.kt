package pt.controleobras.app.core.validation

import pt.controleobras.app.core.model.TalaoDraft
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Valida todos os campos de um [TalaoDraft] antes de serem apresentados ao utilizador.
 *
 * Cada campo recebe um [FieldValidation] com estado VALID / SUSPECT / MISSING.
 * Nunca bloqueia a gravação — é o utilizador que decide o que fazer com campos suspeitos.
 *
 * Regras aplicadas:
 *  - NIF: 9 dígitos, primeiro dígito válido, checksum módulo 11
 *  - Data: data real, não no futuro, não anterior a 2000
 *  - Valores: numérico positivo + cross-validation (subtotal + IVA ≈ total)
 *  - Taxa IVA por linha: apenas 6%, 13% ou 23% (taxas legais PT)
 *  - Total por linha: quantidade × preço ≈ total (tolerância ±0.02€)
 */
@Singleton
class InvoiceFieldValidator @Inject constructor(
    private val nifValidator: NifValidator
) {

    /**
     * Valida [draft] e devolve um mapa de nome-de-campo → [FieldValidation].
     *
     * @param temQr  true quando o QR code AT foi detetado e usado na extração.
     *               Quando false, campos críticos que o QR normalmente garante
     *               (NIF, IVA, Total, Data, Nº Fatura, NIF cliente) ficam SUSPECT
     *               mesmo se preenchidos — indicando que vieram apenas do OCR.
     */
    fun validate(draft: TalaoDraft, temQr: Boolean = false): Map<String, FieldValidation> = buildMap {
        put("empresa",         validateTexto(draft.empresa, "Nome da empresa"))
        put("nif",             validateNif(draft.nif, "NIF do fornecedor"))
        put("nifCliente",      validateNif(draft.nifCliente, "NIF do cliente (vosso NIF)"))
        put("morada",          validateTexto(draft.morada, "Morada"))
        put("data",            validateData(draft.data))
        put("hora",            if (draft.hora != null) FieldValidation.valid() else FieldValidation.missing())
        put("numeroFatura",    validateNumeroFatura(draft.numeroFatura))
        put("metodoPagamento", validateMetodoPagamento(draft.metodoPagamento))
        put("iva",             validateValorMonetario(draft.iva, "IVA"))
        put("total",           validateValorMonetario(draft.total, "Total"))

        val iva   = extrairDouble(draft.iva)
        val total = extrairDouble(draft.total)

        // Cross-validation 1: IVA não pode ser ≥ Total
        if (iva != null && total != null && iva > 0 && total > 0 && iva >= total) {
            put("iva", FieldValidation.suspect("IVA (${draft.iva}€) ≥ Total (${draft.total}€) — verifique na fatura"))
        }

        // Cross-validation 2: soma dos totais de linha ≈ total da fatura
        // Só corre se temos itens com total preenchido E total da fatura conhecido
        val totaisItens = draft.itens.mapNotNull { extrairDouble(it.total.replace(',', '.')) }
        if (totaisItens.size >= 2 && total != null && total > 0) {
            val somaItens = totaisItens.sum()
            // Tolerância de 0.10€ por arredondamentos e descontos globais
            if (Math.abs(somaItens - total) > 0.10) {
                put("produtos", FieldValidation.suspect(
                    "Soma dos artigos (${"%.2f".format(somaItens)}€) ≠ Total (${draft.total}€) — podem faltar artigos ou haver desconto"
                ))
            }
        }

        // Sem QR code AT: campos críticos que o QR normalmente garante ficam SUSPECT
        // mesmo que o OCR tenha encontrado um valor — indica baixa confiança
        if (!temQr) {
            val avisoSemQr = "Informação pouco segura — falta de QR code AT. Confirme na fatura original."
            listOf("nif", "nifCliente", "data", "numeroFatura", "iva", "total").forEach { campo ->
                val atual = get(campo)
                // Apenas downgrade de VALID → SUSPECT; MISSING permanece MISSING
                if (atual?.state == FieldState.VALID) {
                    put(campo, FieldValidation.suspect(avisoSemQr))
                }
            }
        }

        // Validação dos itens de linha individualmente
        draft.itens.forEachIndexed { idx, item ->
            val qtd   = extrairDouble(item.quantidade.replace(',', '.'))
            val preco = extrairDouble(item.precoUnitario.replace(',', '.'))
            val tot   = extrairDouble(item.total.replace(',', '.'))

            if (item.taxaIva.isNotBlank()) {
                val taxa = item.taxaIva.trim().removeSuffix("%").trim().toIntOrNull()
                if (taxa != null && taxa !in listOf(6, 13, 23)) {
                    put("item_${idx}_taxaIva",
                        FieldValidation.suspect("Taxa IVA ${item.taxaIva}% não é taxa legal PT (6%, 13% ou 23%)"))
                }
            }

            if (qtd != null && preco != null && tot != null && qtd > 0 && preco > 0 && tot > 0) {
                val esperado = qtd * preco
                if (Math.abs(esperado - tot) > 0.02) {
                    put("item_${idx}_total",
                        FieldValidation.suspect(
                            "Total da linha (${item.total}€) difere de Qtd×Preço (${"%.2f".format(esperado)}€)"
                        ))
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validadores individuais
    // ─────────────────────────────────────────────────────────────────────────

    private fun validateNif(valor: String, rotulo: String): FieldValidation {
        if (valor.isBlank()) return FieldValidation.missing("$rotulo não encontrado")
        val motivo = nifValidator.motivoInvalido(valor)
        return if (motivo == null) FieldValidation.valid()
        else FieldValidation.suspect(motivo)
    }

    private fun validateTexto(valor: String, rotulo: String): FieldValidation {
        return if (valor.isBlank()) FieldValidation.missing("$rotulo não encontrado")
        else FieldValidation.valid()
    }

    private fun validateData(data: LocalDate?): FieldValidation {
        if (data == null) return FieldValidation.missing("Data não encontrada")
        val hoje = LocalDate.now()
        return when {
            data.isBefore(LocalDate.of(2000, 1, 1)) ->
                FieldValidation.suspect("Data anterior a 2000 — verifique na fatura")
            data.isAfter(hoje.plusDays(3)) ->
                FieldValidation.suspect("Data no futuro ($data) — verifique na fatura")
            else -> FieldValidation.valid()
        }
    }

    private fun validateNumeroFatura(valor: String): FieldValidation {
        if (valor.isBlank()) return FieldValidation.missing("Número de fatura não encontrado")
        // Verifica prefixo documental AT
        val prefixosAt = listOf("FT", "FS", "FR", "FA", "NC", "ND", "GT", "OR", "DC", "RP")
        val temPrefixo = prefixosAt.any { valor.uppercase().startsWith(it) }
        val temSeparador = valor.contains('/') || valor.contains('-')
        return when {
            temPrefixo && temSeparador -> FieldValidation.valid()
            temSeparador -> FieldValidation.valid() // série/número sem prefixo AT — aceitável
            else -> FieldValidation.suspect("Formato de número de fatura não reconhecido")
        }
    }

    private fun validateValorMonetario(valor: String, rotulo: String): FieldValidation {
        if (valor.isBlank()) return FieldValidation.missing("$rotulo não encontrado")
        val num = extrairDouble(valor.replace(',', '.'))
            ?: return FieldValidation.suspect("$rotulo não é um valor numérico válido")
        return when {
            num < 0          -> FieldValidation.suspect("$rotulo é negativo — verifique na fatura")
            num > 99_999.99  -> FieldValidation.suspect("$rotulo (${valor}€) parece demasiado elevado — verifique na fatura")
            else             -> FieldValidation.valid()
        }
    }

    private fun validateMetodoPagamento(valor: String): FieldValidation {
        if (valor.isBlank()) return FieldValidation.missing()
        return FieldValidation.valid()
    }

    private fun extrairDouble(valor: String?): Double? =
        valor?.replace(',', '.')?.trim()?.toDoubleOrNull()
}
