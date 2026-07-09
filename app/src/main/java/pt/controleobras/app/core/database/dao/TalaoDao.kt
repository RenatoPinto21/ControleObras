package pt.controleobras.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pt.controleobras.app.core.database.entity.TalaoEntity

@Dao
interface TalaoDao {

    @Insert
    suspend fun insert(talao: TalaoEntity): Long

    @Query("SELECT * FROM talao ORDER BY criadoEm DESC")
    fun observeAll(): Flow<List<TalaoEntity>>

    @Query("SELECT * FROM talao WHERE id = :id")
    suspend fun getById(id: Long): TalaoEntity?

    @Delete
    suspend fun delete(talao: TalaoEntity)
}
