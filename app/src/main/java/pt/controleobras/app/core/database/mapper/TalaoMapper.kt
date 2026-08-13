package pt.controleobras.app.core.database.mapper

import pt.controleobras.app.core.database.entity.ItemTalaoDto
import pt.controleobras.app.core.database.entity.TalaoEntity
import pt.controleobras.app.core.model.ItemTalao
import pt.controleobras.app.core.model.Talao
import java.math.BigDecimal

fun TalaoEntity.toDomain(): Talao = Talao(
    id              = id,
    empresa         = empresa,
    nif             = nif,
    nifCliente      = nifCliente,
    morada          = morada,
    data            = data,
    hora            = hora,
    serie           = serie,
    numeroFatura    = numeroFatura,
    dataVencimento  = dataVencimento,
    metodoPagamento = metodoPagamento,
    itens           = itens.map { it.toDomain() },
    iva             = iva,
    total           = total,
    observacoes     = observacoes,
    imagemPath      = imagemPath,
    criadoEm        = criadoEm,
    textoOcr        = textoOcr,
    funcn           = funcn,
    fref            = fref,
    nmfref          = nmfref,
    agnome          = agnome
)

fun Talao.toEntity(): TalaoEntity = TalaoEntity(
    id              = id,
    empresa         = empresa,
    nif             = nif,
    nifCliente      = nifCliente,
    morada          = morada,
    data            = data,
    hora            = hora,
    serie           = serie,
    numeroFatura    = numeroFatura,
    dataVencimento  = dataVencimento,
    metodoPagamento = metodoPagamento,
    itens           = itens.map { it.toDto() },
    iva             = iva,
    total           = total,
    observacoes     = observacoes,
    imagemPath      = imagemPath,
    criadoEm        = criadoEm,
    textoOcr        = textoOcr,
    funcn           = funcn,
    fref            = fref,
    nmfref          = nmfref,
    agnome          = agnome
)

fun ItemTalaoDto.toDomain(): ItemTalao = ItemTalao(
    descricao     = descricao,
    quantidade    = BigDecimal(quantidade),
    precoUnitario = BigDecimal(precoUnitario),
    desconto      = desconto.ifBlank { null }?.let { runCatching { BigDecimal(it) }.getOrNull() },
    taxaIva       = taxaIva.ifBlank { null }?.let { runCatching { BigDecimal(it) }.getOrNull() },
    total         = BigDecimal(total)
)

fun ItemTalao.toDto(): ItemTalaoDto = ItemTalaoDto(
    descricao     = descricao,
    quantidade    = quantidade.toPlainString(),
    precoUnitario = precoUnitario.toPlainString(),
    desconto      = desconto?.toPlainString() ?: "",
    taxaIva       = taxaIva?.toPlainString() ?: "",
    total         = total.toPlainString()
)
