package pt.controleobras.app.core.export

import pt.controleobras.app.core.model.Talao

/**
 * Exportação CSV — uma linha por produto. Separador ';' (compatível com o
 * Excel em Portugal, onde ',' já é o separador decimal). Sem dependência
 * externa — formato simples, controlo total.
 */
object CsvExporter {

    private const val SEPARADOR = ";"
    private val CABECALHO = listOf(
        "Empresa", "NIF", "Data", "NumeroFatura",
        "Produto", "Quantidade", "PrecoUnitario", "TotalProduto",
        "IVA", "TotalTalao"
    ).joinToString(SEPARADOR)

    fun toCsv(talao: Talao): String = buildString {
        appendLine(CABECALHO)
        if (talao.itens.isEmpty()) {
            appendLine(linha(talao, produto = null, quantidade = null, precoUnitario = null, totalProduto = null))
        } else {
            talao.itens.forEach { item ->
                appendLine(
                    linha(
                        talao = talao,
                        produto = item.descricao,
                        quantidade = item.quantidade.toPlainString(),
                        precoUnitario = item.precoUnitario.toPlainString(),
                        totalProduto = item.total.toPlainString()
                    )
                )
            }
        }
    }.trimEnd('\n')

    private fun linha(
        talao: Talao,
        produto: String?,
        quantidade: String?,
        precoUnitario: String?,
        totalProduto: String?
    ): String = listOf(
        talao.empresa,
        talao.nif.orEmpty(),
        talao.data?.toString().orEmpty(),
        talao.numeroFatura.orEmpty(),
        produto.orEmpty(),
        quantidade.orEmpty(),
        precoUnitario.orEmpty(),
        totalProduto.orEmpty(),
        talao.iva?.toPlainString().orEmpty(),
        talao.total?.toPlainString().orEmpty()
    ).joinToString(SEPARADOR) { escapar(it) }

    private fun escapar(campo: String): String =
        if (campo.contains(SEPARADOR) || campo.contains('"') || campo.contains('\n')) {
            "\"${campo.replace("\"", "\"\"")}\""
        } else {
            campo
        }
}
