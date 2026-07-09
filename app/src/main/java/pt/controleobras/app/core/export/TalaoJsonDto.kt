package pt.controleobras.app.core.export

import kotlinx.serialization.Serializable
import pt.controleobras.app.core.database.entity.ItemTalaoDto
import pt.controleobras.app.core.database.mapper.toDto
import pt.controleobras.app.core.model.Talao

/**
 * Representação serializável de [Talao] usada apenas para a exportação JSON
 * (ficheiro gravado ao lado da imagem original).
 */
@Serializable
data class TalaoJsonDto(
    val id: Long,
    val empresa: String,
    val nif: String?,
    val morada: String?,
    val data: String?,
    val hora: String?,
    val numeroFatura: String?,
    val itens: List<ItemTalaoDto>,
    val iva: String?,
    val total: String?,
    val observacoes: String?,
    val imagemPath: String,
    val criadoEm: String,
    val textoOcr: String?
) {
    companion object {
        fun fromDomain(talao: Talao): TalaoJsonDto = TalaoJsonDto(
            id = talao.id,
            empresa = talao.empresa,
            nif = talao.nif,
            morada = talao.morada,
            data = talao.data?.toString(),
            hora = talao.hora?.toString(),
            numeroFatura = talao.numeroFatura,
            itens = talao.itens.map { it.toDto() },
            iva = talao.iva?.toPlainString(),
            total = talao.total?.toPlainString(),
            observacoes = talao.observacoes,
            imagemPath = talao.imagemPath,
            criadoEm = talao.criadoEm.toString(),
            textoOcr = talao.textoOcr
        )
    }
}
