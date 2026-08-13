package pt.controleobras.app.core.database.remote

import android.util.Log
import pt.controleobras.app.core.config.ConfigManager
import java.sql.Connection
import java.sql.DriverManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gere a ligação JDBC direta ao servidor MariaDB.
 *
 * As credenciais são lidas do [ConfigManager] (desencriptadas em runtime).
 * Cada chamada a [obterLigacao] cria uma nova ligação — não existe pool de ligações
 * (adequado para sincronizações pontuais, não para queries contínuas).
 *
 * IMPORTANTE: chamar sempre em [kotlinx.coroutines.Dispatchers.IO].
 */
@Singleton
class RemoteDatabaseManager @Inject constructor(
    private val configManager: ConfigManager
) {
    companion object {
        private const val TAG = "RemoteDB"
    }

    /**
     * Cria e devolve uma nova ligação ao MariaDB.
     * O chamador é responsável por fechar a ligação após uso (usar `use {}`).
     *
     * @throws Exception se as credenciais forem inválidas ou o servidor inacessível.
     */
    fun obterLigacao(): Connection {
        // Regista o driver explicitamente — necessário no Android (sem ServiceLoader)
        Class.forName("org.mariadb.jdbc.Driver")

        val config = configManager.obterConfig()
        val url    = "jdbc:mariadb://${config.servidor}:${config.porta}/${config.baseDados}"

        Log.d(TAG, "A iniciar ligação JDBC…")
        return try {
            DriverManager.getConnection(url, config.login, config.password)
                .also { Log.d(TAG, "Ligação estabelecida com sucesso") }
        } catch (e: Exception) {
            Log.e(TAG, "Falha na ligação JDBC: ${e.javaClass.simpleName}")
            throw e
        }
    }
}
