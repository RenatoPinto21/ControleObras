package pt.controleobras.app.core.database.entity

import kotlinx.serialization.Serializable

/**
 * Representação serializável de [pt.controleobras.app.core.model.ItemTalao]
 * usada apenas para persistência (Room TypeConverter) e exportação JSON.
 */
@Serializable
data class ItemTalaoDto(
    val descricao: String,
    val quantidade: String,
    val precoUnitario: String,
    val total: String
)
