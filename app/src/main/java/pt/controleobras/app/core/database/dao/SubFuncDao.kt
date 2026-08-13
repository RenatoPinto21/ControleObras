package pt.controleobras.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import pt.controleobras.app.core.database.entity.SubFuncEntity

@Dao
interface SubFuncDao {

    /** Devolve funcionários de um centro de custo específico. */
    @Query("SELECT * FROM subfunc WHERE fref = :fref ORDER BY nome ASC")
    fun listarPorFref(fref: String): Flow<List<SubFuncEntity>>

    @Query("DELETE FROM subfunc WHERE fref = :fref")
    suspend fun limparPorFref(fref: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(lista: List<SubFuncEntity>)

    /** Sync atómico por centro de custo: apaga e re-insere. */
    @Transaction
    suspend fun substituirPorFref(fref: String, lista: List<SubFuncEntity>) {
        limparPorFref(fref)
        inserirTodos(lista)
    }
}
