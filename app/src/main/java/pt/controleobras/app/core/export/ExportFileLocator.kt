package pt.controleobras.app.core.export

import android.content.Context
import java.io.File

/**
 * Convenção única de nomes/caminhos dos ficheiros de exportação de um talão.
 * Usado pelo repositório (para escrever) e pelos ecrãs (para partilhar),
 * evitando duplicar a construção do caminho em vários sítios.
 */
object ExportFileLocator {

    fun pasta(context: Context): File = File(context.filesDir, "receipts").apply { mkdirs() }

    fun ficheiroJson(context: Context, talaoId: Long): File = File(pasta(context), "talao_$talaoId.json")
    fun ficheiroXml(context: Context, talaoId: Long): File = File(pasta(context), "talao_$talaoId.xml")
    fun ficheiroCsv(context: Context, talaoId: Long): File = File(pasta(context), "talao_$talaoId.csv")
}
