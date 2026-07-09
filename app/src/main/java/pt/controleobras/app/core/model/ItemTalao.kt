package pt.controleobras.app.core.model

import java.math.BigDecimal

/**
 * Linha de produto de um talão, já validada e confirmada pelo utilizador.
 */
data class ItemTalao(
    val descricao: String,
    val quantidade: BigDecimal,
    val precoUnitario: BigDecimal,
    val total: BigDecimal
)
