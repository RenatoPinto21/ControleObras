package pt.controleobras.app.core.model

import java.math.BigDecimal

/**
 * Linha de produto/serviço de um talão, já validada e confirmada pelo utilizador.
 */
data class ItemTalao(
    val descricao: String,
    val quantidade: BigDecimal,
    val precoUnitario: BigDecimal,

    /** Desconto na linha (null se não aplicável). */
    val desconto: BigDecimal? = null,

    /** Taxa de IVA da linha em % (ex: 23.00, 13.00, 6.00). */
    val taxaIva: BigDecimal? = null,

    val total: BigDecimal
)
