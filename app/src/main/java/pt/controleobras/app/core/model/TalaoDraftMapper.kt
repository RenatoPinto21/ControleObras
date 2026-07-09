package pt.controleobras.app.core.model

import java.math.BigDecimal

/**
 * Converte o rascunho editado pelo utilizador no modelo de domínio final,
 * validando o mínimo indispensável (empresa e preço unitário de cada item).
 */
fun TalaoDraft.paraDominio(): Talao {
    require(empresa.isNotBlank()) { "A empresa é obrigatória." }
    return Talao(
        empresa = empresa.trim(),
        nif = nif.trim().ifBlank { null },
        morada = morada.trim().ifBlank { null },
        data = data,
        hora = hora,
        numeroFatura = numeroFatura.trim().ifBlank { null },
        itens = itens.mapNotNull { it.paraDominioOuNulo() },
        iva = iva.paraBigDecimalOuNulo(),
        total = total.paraBigDecimalOuNulo(),
        observacoes = observacoes.trim().ifBlank { null },
        imagemPath = imagemPath,
        textoOcr = textoReconhecido.ifBlank { null }
    )
}

private fun ItemTalaoDraft.paraDominioOuNulo(): ItemTalao? {
    if (descricao.isBlank()) return null
    val precoUnitario = precoUnitario.paraBigDecimalOuNulo() ?: return null
    val quantidade = quantidade.paraBigDecimalOuNulo() ?: BigDecimal.ONE
    val total = total.paraBigDecimalOuNulo() ?: (quantidade * precoUnitario)
    return ItemTalao(descricao.trim(), quantidade, precoUnitario, total)
}

private fun String.paraBigDecimalOuNulo(): BigDecimal? =
    trim().takeIf { it.isNotEmpty() }?.let { runCatching { BigDecimal(it) }.getOrNull() }
