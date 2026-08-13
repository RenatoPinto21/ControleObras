package pt.controleobras.app.core.database.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pt.controleobras.app.core.database.dao.CentroCustoDao
import pt.controleobras.app.core.database.entity.CentroCustoEntity
import pt.controleobras.app.core.model.CentroCusto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório de Centros de Custo.
 *
 * Fonte de verdade: tabela FREF do MariaDB (PHPRetailConcept).
 * Cache local: tabela [CentroCustoEntity] no Room.
 *
 * Fluxo:
 *   1. [sincronizar] — conecta ao MariaDB, filtra por ENCSERIE ou SINCTAB=1,
 *                       guarda resultado no Room.
 *   2. [listarTodos] — devolve Flow do Room (funciona offline após primeira sync).
 */
@Singleton
class FrefRepository @Inject constructor(
    private val remoteDb: RemoteDatabaseManager,
    private val dao: CentroCustoDao
) {
    companion object {
        private const val TAG = "FrefRepository"

        /**
         * Tabela com nome completo — o utilizador 'jao' tem acesso cross-database.
         * SINCTAB é tipo BIT no MariaDB — usar != 0 para compatibilidade JDBC.
         */
        private const val SQL = """
            SELECT FREF, NMFREF, AGNOME
            FROM PHPRetailConcept.FREF
            WHERE ENCSERIE = ? OR SINCTAB != 0
            ORDER BY FREF ASC
        """
    }

    /**
     * Lista todos os centros de custo em cache (Room).
     * Atualizado automaticamente após cada [sincronizar].
     */
    fun listarTodos(): Flow<List<CentroCusto>> =
        dao.listarTodos().map { lista -> lista.map { it.toDomain() } }

    /**
     * Sincroniza a lista de centros de custo com o servidor.
     *
     * @param serialDispositivo Android ID do dispositivo (ANDROID_ID uppercase)
     *                          — filtra registos ENCSERIE específicos deste tablet.
     * @return [ResultadoSync] com estado e contagem de registos obtidos.
     */
    suspend fun sincronizar(serialDispositivo: String): ResultadoSync =
        withContext(Dispatchers.IO) {
            runCatching {
                remoteDb.obterLigacao().use { conn ->
                    conn.prepareStatement(SQL.trimIndent()).use { stmt ->
                        stmt.setString(1, serialDispositivo)
                        val rs     = stmt.executeQuery()
                        val lista  = mutableListOf<CentroCustoEntity>()
                        while (rs.next()) {
                            // trim() — MariaDB CHAR devolve valores com padding
                            val entity = CentroCustoEntity(
                                fref   = rs.getString("FREF")?.trim()   ?: continue,
                                nmfref = rs.getString("NMFREF")?.trim() ?: "",
                                agnome = rs.getString("AGNOME")?.trim() ?: ""
                            )
                            lista += entity
                            Log.d(TAG, "  CC: ${entity.fref.trim()} — ${entity.nmfref.trim()}")
                        }
                        rs.close()
                        dao.substituirTodos(lista)
                        Log.d(TAG, "Sync OK — ${lista.size} centros de custo")
                        ResultadoSync.Sucesso(lista.size)
                    }
                }
            }.getOrElse { erro ->
                Log.e(TAG, "Falha na sincronização: ${erro.javaClass.simpleName}")
                ResultadoSync.Erro("${erro.javaClass.simpleName}: ${erro.message ?: "Erro desconhecido"}")
            }
        }
}

sealed class ResultadoSync {
    data class Sucesso(val total: Int) : ResultadoSync()
    data class Erro(val mensagem: String) : ResultadoSync()
}
