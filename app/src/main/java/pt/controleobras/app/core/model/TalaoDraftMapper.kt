package pt.controleobras.app.core.model

import java.math.BigDecimal

/**
 * Converte o rascunho editado pelo utilizador no modelo de domínio final,
 * validando o mínimo indispensável (empresa não vazia e preço unitário de cada item).
 */
fun TalaoDraft.paraDominio(): Talao {
    require(empresa.isNotBlank()) { "A empresa é obrigatória." }
    return Talao(
        empresa          = empresa.trim(),
        nif              = nif.trim().ifBlank { null },
        nifCliente       = nifCliente.trim().ifBlank { null },
        morada           = morada.trim().ifBlank { null },
        data             = data,
        hora             = hora,
        serie            = serie.trim().ifBlank { null },
        numeroFatura     = numeroFatura.trim().ifBlank { null },
        dataVencimento   = dataVencimento,
        metodoPagamento  = metodoPagamento.trim().ifBlank { null },
        itens            = itens.mapNotNull { it.paraDominioOuNulo() },
        iva              = iva.paraBigDecimalOuNulo(),
        total            = total.paraBigDecimalOuNulo(),
        observacoes      = observacoes.trim().ifBlank { null },
        imagemPath       = imagemPath,
        textoOcr         = textoReconhecido.ifBlank { null }
    )
}

private fun ItemTalaoDraft.paraDominioOuNulo(): ItemTalao? {
    if (descricao.isBlank()) return null
    val precoUnit = precoUnitario.paraBigDecimalOuNulo() ?: return null
    val qtd       = quantidade.paraBigDecimalOuNulo() ?: BigDecimal.ONE
    val tot       = total.paraBigDecimalOuNulo() ?: (qtd * precoUnit)
    return ItemTalao(
        descricao     = descricao.trim(),
        quantidade    = qtd,
        precoUnitario = precoUnit,
        desconto      = desconto.paraBigDecimalOuNulo(),
        taxaIva       = taxaIva.paraBigDecimalOuNulo(),
        total         = tot
    )
}

private fun String.paraBigDecimalOuNulo(): BigDecimal? =
    trim().takeIf { it.isNotEmpty() }?.let { runCatching { BigDecimal(it) }.getOrNull() }
