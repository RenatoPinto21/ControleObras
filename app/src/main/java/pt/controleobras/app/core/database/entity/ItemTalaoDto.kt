package pt.controleobras.app.core.database.entity

import kotlinx.serialization.Serializable

/**
 * Representação serializável de [pt.controleobras.app.core.model.ItemTalao]
 * usada apenas para persistência (Room TypeConverter) e exportação JSON.
 *
 * Campos com valor por omissão "" para compatibilidade com registos antigos
 * (deserialização de JSON gravado antes desta versão não falha).
 */
@Serializable
data class ItemTalaoDto(
    val descricao: String,
    val quantidade: String,
    val precoUnitario: String,
    val desconto: String = "",
    val taxaIva: String = "",
    val total: String
)
