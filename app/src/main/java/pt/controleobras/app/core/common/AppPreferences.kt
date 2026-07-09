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

    private companion object {
        const val CHAVE_BOAS_VINDAS = "ja_viu_boas_vindas"
        const val CHAVE_DRIVE_FOLDER = "drive_folder_uri"
    }
}
