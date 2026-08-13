package pt.controleobras.app.core.common

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferências da app protegidas com [EncryptedSharedPreferences].
 *
 * Utiliza AES-256 via Android Keystore (MasterKey) para encriptar
 * tanto as chaves como os valores. Transparente para o chamador.
 *
 * Na primeira execução após a migração de segurança, os dados do ficheiro
 * antigo (controle_obras_prefs) são copiados para o novo ficheiro encriptado
 * e o ficheiro antigo é eliminado — sem perda de dados para o utilizador.
 *
 * Se a criação das prefs encriptadas falhar (dispositivos muito antigos
 * ou Keystore corrompido), cai para SharedPreferences simples com log
 * de aviso — garante que a app não crasha.
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = criarPrefsSeguras(context).also { novasPrefs ->
        migrarPrefsAntigas(context, novasPrefs)
    }

    var jaViuBoasVindas: Boolean
        get() = prefs.getBoolean(CHAVE_BOAS_VINDAS, false)
        set(value) = prefs.edit { putBoolean(CHAVE_BOAS_VINDAS, value) }

    var driveFolderUri: String?
        get() = prefs.getString(CHAVE_DRIVE_FOLDER, null)
        set(value) = prefs.edit { putString(CHAVE_DRIVE_FOLDER, value) }

    var llmDownloadId: Long
        get() = prefs.getLong(CHAVE_LLM_DOWNLOAD_ID, -1L)
        set(value) = prefs.edit { putLong(CHAVE_LLM_DOWNLOAD_ID, value) }

    var ultimaFaturaFeedback: String?
        get() = prefs.getString(CHAVE_ULTIMA_FATURA, null)
        set(value) = prefs.edit { putString(CHAVE_ULTIMA_FATURA, value) }

    /** Timestamp (millis) da última sincronização bem-sucedida com a BD. */
    var ultimaSyncTimestamp: Long
        get() = prefs.getLong(CHAVE_ULTIMA_SYNC, 0L)
        set(value) = prefs.edit { putLong(CHAVE_ULTIMA_SYNC, value) }

    private companion object {
        const val TAG = "AppPreferences"
        const val NOME_PREFS = "controle_obras_prefs_enc"
        const val NOME_PREFS_ANTIGO = "controle_obras_prefs"
        const val CHAVE_BOAS_VINDAS     = "ja_viu_boas_vindas"
        const val CHAVE_DRIVE_FOLDER    = "drive_folder_uri"
        const val CHAVE_LLM_DOWNLOAD_ID = "llm_download_id"
        const val CHAVE_ULTIMA_FATURA   = "ultima_fatura_feedback"
        const val CHAVE_ULTIMA_SYNC     = "ultima_sync_timestamp"

        fun criarPrefsSeguras(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    NOME_PREFS,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao criar EncryptedSharedPreferences, a usar fallback", e)
                context.getSharedPreferences(NOME_PREFS, Context.MODE_PRIVATE)
            }
        }

        /**
         * Migra dados do ficheiro antigo (não encriptado) para o novo.
         * Executa apenas uma vez — se o ficheiro antigo existir, copia os valores
         * e elimina-o. Idempotente: se o ficheiro antigo não existir, não faz nada.
         */
        fun migrarPrefsAntigas(context: Context, novasPrefs: SharedPreferences) {
            val ficheiroAntigo = java.io.File(
                context.applicationInfo.dataDir + "/shared_prefs/$NOME_PREFS_ANTIGO.xml"
            )
            if (!ficheiroAntigo.exists()) return

            try {
                val antigas = context.getSharedPreferences(NOME_PREFS_ANTIGO, Context.MODE_PRIVATE)
                val todosDados = antigas.all
                if (todosDados.isEmpty()) {
                    ficheiroAntigo.delete()
                    return
                }

                novasPrefs.edit {
                    todosDados.forEach { (chave, valor) ->
                        when (valor) {
                            is Boolean -> putBoolean(chave, valor)
                            is String  -> putString(chave, valor)
                            is Long    -> putLong(chave, valor)
                            is Int     -> putInt(chave, valor)
                            is Float   -> putFloat(chave, valor)
                        }
                    }
                }

                // Limpar e eliminar o ficheiro antigo (não encriptado)
                antigas.edit { clear() }
                ficheiroAntigo.delete()
                Log.d(TAG, "Migração de prefs concluída — ${todosDados.size} valores migrados")
            } catch (e: Exception) {
                Log.w(TAG, "Falha na migração de prefs antigas (dados não perdidos)", e)
            }
        }
    }
}
