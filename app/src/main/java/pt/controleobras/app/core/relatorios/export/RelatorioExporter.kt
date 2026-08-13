package pt.controleobras.app.core.relatorios.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import pt.controleobras.app.core.relatorios.model.RelatorioDespesas
import pt.controleobras.app.core.relatorios.model.RelatorioPresencas
import pt.controleobras.app.core.relatorios.model.RelatorioPresencasReg
import java.io.File
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Gera e partilha relatórios de Despesas e Presenças em PDF ou CSV.
 * Usa [android.graphics.pdf.PdfDocument] — sem dependências externas.
 */
class RelatorioExporter @Inject constructor() {

    private val fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // ── PDF ──────────────────────────────────────────────────────────────────

    fun exportarDespesasPdf(context: Context, relatorio: RelatorioDespesas) {
        val titulo = "Relatório de Despesas — ${relatorio.data.format(fmtData)}"
        val cabecalho = listOf("Hora", "Empresa", "NIF", "Nº Fatura", "Func.", "Obra", "Total (€)")
        val linhas = relatorio.linhas.map { l ->
            listOf(l.hora, l.empresa.take(30), l.nif, l.numeroFatura, l.funcn, l.fref, "%.2f".format(l.total))
        }
        val rodape = "TOTAL GERAL: %.2f €".format(relatorio.totalGeral)
        val ficheiro = File(context.cacheDir, "despesas_${relatorio.data}.pdf")
        gerarPdf(ficheiro, titulo, cabecalho, linhas, rodape)
        partilhar(context, ficheiro, "application/pdf")
    }

    fun exportarPresencasPdf(context: Context, relatorio: RelatorioPresencas) {
        val titulo = "Relatório de Presenças — ${relatorio.data.format(fmtData)}"
        val cabecalho = listOf("Func.", "Obra (FREF)", "Nome Obra", "Encarregado", "Talões")
        val linhas = relatorio.linhas.map { l ->
            listOf(l.funcn, l.fref, l.nmfref.take(30), l.agnome.take(20), l.totalTaloes.toString())
        }
        val rodape = "Total de presenças: ${relatorio.linhas.size}"
        val ficheiro = File(context.cacheDir, "presencas_${relatorio.data}.pdf")
        gerarPdf(ficheiro, titulo, cabecalho, linhas, rodape)
        partilhar(context, ficheiro, "application/pdf")
    }

    // ── CSV ──────────────────────────────────────────────────────────────────

    fun exportarDespesasCsv(context: Context, relatorio: RelatorioDespesas) {
        val sb = StringBuilder()
        sb.appendLine("HORA;EMPRESA;NIF;NUMERO_FATURA;FUNCN;FREF;NMFREF;TOTAL")
        relatorio.linhas.forEach { l ->
            sb.appendLine("${l.hora};${l.empresa};${l.nif};${l.numeroFatura};${l.funcn};${l.fref};${l.nmfref};${"%.2f".format(l.total)}")
        }
        sb.appendLine(";;;;;;;${"%.2f".format(relatorio.totalGeral)}")
        val ficheiro = File(context.cacheDir, "despesas_${relatorio.data}.csv")
        ficheiro.writeText(sb.toString(), Charsets.UTF_8)
        partilhar(context, ficheiro, "text/csv")
    }

    fun exportarPresencasCsv(context: Context, relatorio: RelatorioPresencas) {
        val sb = StringBuilder()
        sb.appendLine("FUNCN;FREF;NMFREF;AGNOME;TOTAL_TALOES")
        relatorio.linhas.forEach { l ->
            sb.appendLine("${l.funcn};${l.fref};${l.nmfref};${l.agnome};${l.totalTaloes}")
        }
        val ficheiro = File(context.cacheDir, "presencas_${relatorio.data}.csv")
        ficheiro.writeText(sb.toString(), Charsets.UTF_8)
        partilhar(context, ficheiro, "text/csv")
    }

    // ── Exportação de presenças registadas (SUBFUNC_REG) ─────────────────────

    fun exportarPresencasRegPdf(context: Context, relatorio: RelatorioPresencasReg) {
        val filtroLabel = relatorio.frefFiltro?.let { " (CC $it)" } ?: ""
        val titulo = "Presenças Registadas — ${relatorio.data.format(fmtData)}$filtroLabel"
        val cabecalho = listOf("Nome", "Função", "CC", "Nome Obra", "Hora", "Obs")
        val linhas = relatorio.linhas.map { l ->
            listOf(l.nome.take(25), l.designacao.take(20), l.fref, l.nmfref.take(25), l.hora, l.obs.take(15))
        }
        val rodape = "Total de presenças: ${relatorio.linhas.size}"
        val ficheiro = File(context.cacheDir, "presencas_reg_${relatorio.data}.pdf")
        gerarPdf(ficheiro, titulo, cabecalho, linhas, rodape)
        partilhar(context, ficheiro, "application/pdf")
    }

    fun exportarPresencasRegCsv(context: Context, relatorio: RelatorioPresencasReg) {
        val sb = StringBuilder()
        sb.appendLine("NOME;DESIGNACAO;FREF;NMFREF;HORA;OBS;BISTAMP")
        relatorio.linhas.forEach { l ->
            sb.appendLine("${l.nome};${l.designacao};${l.fref};${l.nmfref};${l.hora};${l.obs};${l.bistamp}")
        }
        val ficheiro = File(context.cacheDir, "presencas_reg_${relatorio.data}.csv")
        ficheiro.writeText(sb.toString(), Charsets.UTF_8)
        partilhar(context, ficheiro, "text/csv")
    }

    // ── Gerador PDF interno ───────────────────────────────────────────────────

    private fun gerarPdf(
        ficheiro:  File,
        titulo:    String,
        cabecalho: List<String>,
        linhas:    List<List<String>>,
        rodape:    String
    ) {
        val pageWidth  = 842 // A4 landscape pts
        val pageHeight = 595
        val margin     = 40f
        val rowH       = 22f
        val colW       = (pageWidth - margin * 2) / cabecalho.size

        val doc  = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(info)
        val canvas: Canvas = page.canvas

        val paintTitulo = Paint().apply {
            typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize  = 14f
            color     = android.graphics.Color.WHITE
        }
        val paintHeader = Paint().apply {
            typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize  = 9f
            color     = android.graphics.Color.WHITE
        }
        val paintCell = Paint().apply {
            textSize = 8f
            color    = android.graphics.Color.parseColor("#CCCCCC")
        }
        val paintLinha = Paint().apply {
            color       = android.graphics.Color.parseColor("#2A2A2A")
            strokeWidth = 0.5f
        }
        val paintFundo = Paint().apply { color = android.graphics.Color.parseColor("#111820") }
        val paintHeader2 = Paint().apply { color = android.graphics.Color.parseColor("#E65100") }
        val paintRodape = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 9f
            color    = android.graphics.Color.parseColor("#FF7043")
        }

        // Fundo
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paintFundo)

        // Barra titulo
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 40f, paintHeader2)
        canvas.drawText(titulo, margin, 26f, paintTitulo)

        // Cabeçalho tabela
        var y = 60f
        canvas.drawRect(margin, y - 14f, pageWidth - margin, y + 6f, paintHeader2.apply { alpha = 180 })
        cabecalho.forEachIndexed { i, col ->
            canvas.drawText(col, margin + i * colW + 4, y, paintHeader)
        }
        y += rowH

        // Linhas
        linhas.forEachIndexed { idx, linha ->
            if (idx % 2 == 0) {
                canvas.drawRect(margin, y - 14f, pageWidth - margin, y + 6f,
                    Paint().apply { color = android.graphics.Color.parseColor("#161C22") })
            }
            linha.forEachIndexed { i, cell ->
                canvas.drawText(cell.take(20), margin + i * colW + 4, y, paintCell)
            }
            canvas.drawLine(margin, y + 6f, pageWidth - margin, y + 6f, paintLinha)
            y += rowH
        }

        // Rodapé
        y += 10f
        canvas.drawText(rodape, margin, y, paintRodape)

        doc.finishPage(page)
        ficheiro.outputStream().use { doc.writeTo(it) }
        doc.close()
    }

    // ── Partilha via Intent ───────────────────────────────────────────────────

    private fun partilhar(context: Context, ficheiro: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            ficheiro
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type     = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Exportar relatório").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })

        // Limpar ficheiros antigos do cache (> 1 hora) para não acumular dados sensíveis
        limparCacheAntigo(context)
    }

    /**
     * Remove ficheiros de relatório do cache com mais de 1 hora.
     * Evita acumulação de PDFs/CSVs com dados financeiros no armazenamento.
     */
    private fun limparCacheAntigo(context: Context) {
        val limiteMs = System.currentTimeMillis() - 3_600_000 // 1 hora
        context.cacheDir.listFiles()?.forEach { ficheiro ->
            if (ficheiro.isFile &&
                (ficheiro.name.startsWith("despesas_") || ficheiro.name.startsWith("presencas_")) &&
                ficheiro.lastModified() < limiteMs
            ) {
                ficheiro.delete()
            }
        }
    }
}
