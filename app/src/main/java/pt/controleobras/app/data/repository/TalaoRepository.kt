package pt.controleobras.app.data.repository

import kotlinx.coroutines.flow.Flow
import pt.controleobras.app.core.model.Talao

interface TalaoRepository {
    suspend fun guardar(talao: Talao): Long
    fun observarTodos(): Flow<List<Talao>>
    suspend fun obterPorId(id: Long): Talao?
}
