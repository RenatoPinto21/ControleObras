package pt.controleobras.app.core.export

import pt.controleobras.app.core.model.Talao

/**
 * Serializador XML manual para [Talao]. Sem dependência externa — o volume
 * de campos é pequeno e fixo, e mantemos controlo total sobre o formato.
 */
object XmlExporter {

    fun toXml(talao: Talao): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("<talao>")
        appendTag("empresa", talao.empresa)
        appendTag("nif", talao.nif)
        appendTag("morada", talao.morada)
        appendTag("data", talao.data?.toString())
        appendTag("hora", talao.hora?.toString())
        appendTag("numeroFatura", talao.numeroFatura)
        appendLine("  <itens>")
        talao.itens.forEach { item ->
            appendLine("    <item>")
            appendTag("descricao", item.descricao, indent = "      ")
            appendTag("quantidade", item.quantidade.toPlainString(), indent = "      ")
            appendTag("precoUnitario", item.precoUnitario.toPlainString(), indent = "      ")
            appendTag("total", item.total.toPlainString(), indent = "      ")
            appendLine("    </item>")
        }
        appendLine("  </itens>")
        appendTag("iva", talao.iva?.toPlainString())
        appendTag("total", talao.total?.toPlainString())
        appendTag("observacoes", talao.observacoes)
        appendTag("imagemPath", talao.imagemPath)
        appendTag("textoOcr", talao.textoOcr)
        append("</talao>")
    }

    private fun StringBuilder.appendTag(nome: String, valor: String?, indent: String = "  ") {
        appendLine("$indent<$nome>${escape(valor.orEmpty())}</$nome>")
    }

    private fun escape(texto: String): String = texto
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
