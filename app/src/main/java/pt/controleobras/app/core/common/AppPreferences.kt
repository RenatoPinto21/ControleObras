package pt.controleobras.app.core.common

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferências simples da app (`SharedPreferences`, sem dependência extra).
 * Guarda apenas flags leves — não é o sítio para dados de domínio.
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("controle_obras_prefs", Context.MODE_PRIVATE)

    var jaViuBoasVindas: Boolean
        get() = prefs.getBoolean(CHAVE_BOAS_VINDAS, false)
        set(value) = prefs.edit { putBoolean(CHAVE_BOAS_VINDAS, value) }

    /** URI da pasta Google Drive selecionada via SAF. Null = não configurado. */
    var driveFolderUri: String?
        get() = prefs.getString(CHAVE_DRIVE_FOLDER, null)
        set(value) = prefs.edit { putString(CHAVE_DRIVE_FOLDER, value) }

    /**
     * ID do download do modelo LLM em curso (DownloadManager).
     * -1L = sem download ativo.
     * Persistido para poder re-verificar o estado após reiniciar a app.
     */
    var llmDownloadId: Long
        get() = prefs.getLong(CHAVE_LLM_DOWNLOAD_ID, -1L)
        set(value) = prefs.edit { putLong(CHAVE_LLM_DOWNLOAD_ID, value) }

    private companion object {
        const val CHAVE_BOAS_VINDAS      = "ja_viu_boas_vindas"
        const val CHAVE_DRIVE_FOLDER     = "drive_folder_uri"
        const val CHAVE_LLM_DOWNLOAD_ID  = "llm_download_id"
    }
}
