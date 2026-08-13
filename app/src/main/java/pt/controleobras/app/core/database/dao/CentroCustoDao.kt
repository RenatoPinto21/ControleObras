package pt.controleobras.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import pt.controleobras.app.core.database.entity.CentroCustoEntity

@Dao
interface CentroCustoDao {

    /** Devolve todos os registos ordenados por código. */
    @Query("SELECT * FROM centro_custo ORDER BY fref ASC")
    fun listarTodos(): Flow<List<CentroCustoEntity>>

    /** Substitui todos os registos existentes pelos novos (sync completo). */
    @Query("DELETE FROM centro_custo")
    suspend fun limparTodos()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(lista: List<CentroCustoEntity>)

    /**
     * Sync atómico: apaga tudo e insere a nova lista numa única transação.
     * Sem @Transaction, o DELETE e o INSERT corriam em transações separadas,
     * o que podia causar perda de dados se o INSERT falhasse.
     */
    @Transaction
    suspend fun substituirTodos(lista: List<CentroCustoEntity>) {
        limparTodos()
        inserirTodos(lista)
    }
}
