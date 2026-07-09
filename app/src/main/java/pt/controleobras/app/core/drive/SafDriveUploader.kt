package pt.controleobras.app.core.drive

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.controleobras.app.core.common.AppPreferences
import java.io.File
import javax.inject.Inject

/**
 * Upload para o Google Drive via Storage Access Framework (SAF).
 *
 * Não requer OAuth, chaves de API nem conta configurada no código.
 * Funciona com qualquer conta Google já configurada no tablet — a app
 * pede ao utilizador que selecione uma pasta do Drive uma única vez
 * (ver [AppPreferences.driveFolderUri]).
 *
 * Mecanismo:
 *  1. O URI da pasta (persisted) identifica a pasta no Drive.
 *  2. [DocumentsContract.createDocument] cria um novo ficheiro nessa pasta.
 *  3. O ContentResolver escreve o conteúdo; o Drive app sincroniza em background.
 */
class SafDriveUploader @Inject constructor(
    private val prefs: AppPreferences
) : DriveUploader {

    override fun isConfigurado(context: Context): Boolean =
        prefs.driveFolderUri != null

    override suspend fun upload(context: Context, file: File, mimeType: String): Boolean =
        withContext(Dispatchers.IO) {
            val folderUriStr = prefs.driveFolderUri ?: return@withContext false
            runCatching {
                // OpenDocumentTree devolve um tree URI (content://.../tree/...).
                // createDocument precisa de um document URI (content://.../document/...).
                val treeUri = Uri.parse(folderUriStr)
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )

                val newDocUri = DocumentsContract.createDocument(
                    context.contentResolver,
                    documentUri,
                    mimeType,
                    file.name
                ) ?: return@withContext false

                context.contentResolver.openOutputStream(newDocUri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
                true
            }.getOrDefault(false)
        }
}
