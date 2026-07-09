package pt.controleobras.app.core.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider

/**
 * Abre o menu de partilha do Android para o ficheiro de exportação (XML/CSV)
 * de um talão. Usado tanto pelo Histórico como pelo ecrã de Detalhe.
 */
fun partilharExportacao(context: Context, talaoId: Long, extensao: String) {
    val ficheiro = if (extensao == "xml") {
        ExportFileLocator.ficheiroXml(context, talaoId)
    } else {
        ExportFileLocator.ficheiroCsv(context, talaoId)
    }
    if (!ficheiro.exists()) return

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", ficheiro)
    val tipoMime = if (extensao == "xml") "application/xml" else "text/csv"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = tipoMime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Exportar talão"))
}
