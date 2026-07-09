package pt.controleobras.app.core.database.mapper

import pt.controleobras.app.core.database.entity.ItemTalaoDto
import pt.controleobras.app.core.database.entity.TalaoEntity
import pt.controleobras.app.core.model.ItemTalao
import pt.controleobras.app.core.model.Talao
import java.math.BigDecimal

fun TalaoEntity.toDomain(): Talao = Talao(
    id = id,
    empresa = empresa,
    nif = nif,
    morada = morada,
    data = data,
    hora = hora,
    numeroFatura = numeroFatura,
    itens = itens.map { it.toDomain() },
    iva = iva,
    total = total,
    observacoes = observacoes,
    imagemPath = imagemPath,
    criadoEm = criadoEm,
    textoOcr = textoOcr
)

fun Talao.toEntity(): TalaoEntity = TalaoEntity(
    id = id,
    empresa = empresa,
    nif = nif,
    morada = morada,
    data = data,
    hora = hora,
    numeroFatura = numeroFatura,
    itens = itens.map { it.toDto() },
    iva = iva,
    total = total,
    observacoes = observacoes,
    imagemPath = imagemPath,
    criadoEm = criadoEm,
    textoOcr = textoOcr
)

fun ItemTalaoDto.toDomain(): ItemTalao = ItemTalao(
    descricao = descricao,
    quantidade = BigDecimal(quantidade),
    precoUnitario = BigDecimal(precoUnitario),
    total = BigDecimal(total)
)

fun ItemTalao.toDto(): ItemTalaoDto = ItemTalaoDto(
    descricao = descricao,
    quantidade = quantidade.toPlainString(),
    precoUnitario = precoUnitario.toPlainString(),
    total = total.toPlainString()
)
