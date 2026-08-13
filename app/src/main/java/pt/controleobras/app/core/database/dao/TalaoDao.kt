package pt.controleobras.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pt.controleobras.app.core.database.entity.TalaoEntity

/**
 * Data Access Object para a tabela de talões/faturas.
 *
 * Todas as operações de leitura e escrita de talões passam por aqui.
 * O Room gera automaticamente a implementação desta interface em tempo de compilação.
 *
 * Convenções:
 *  - Funções que retornam [Flow] observam a BD em tempo real (o Compose redesenha automaticamente)
 *  - Funções [suspend] executam uma vez e retornam o resultado
 *  - Os parâmetros de data são Strings no formato ISO "yyyy-MM-dd" (armazenamento interno do Room)
 */
@Dao
interface TalaoDao {

    /** Insere um novo talão na BD. Retorna o ID gerado automaticamente. */
    @Insert
    suspend fun insert(talao: TalaoEntity): Long

    /**
     * Observa TODOS os talões, ordenados do mais recente para o mais antigo.
     * Usado pela lista de faturas (ReceiptListScreen).
     * O Room emite uma nova lista cada vez que a tabela é modificada.
     */
    @Query("SELECT * FROM talao ORDER BY criadoEm DESC")
    fun observeAll(): Flow<List<TalaoEntity>>

    /** Obtém um talão pelo seu ID. Retorna null se não existir. */
    @Query("SELECT * FROM talao WHERE id = :id")
    suspend fun getById(id: Long): TalaoEntity?

    /** Apaga um talão da BD. Usado pelo ecrã de detalhe (swipe para apagar, etc.). */
    @Delete
    suspend fun delete(talao: TalaoEntity)

    // ── Relatórios ────────────────────────────────────────────────────────────

    /**
     * Todas as datas distintas com pelo menos um talão registado.
     * Usado para marcar os dias no calendário.
     */
    @Query("SELECT DISTINCT data FROM talao WHERE data IS NOT NULL ORDER BY data ASC")
    fun observarDatasComDados(): Flow<List<String>>

    /**
     * Todos os talões de um dia específico, ordenados por hora.
     * Usado no relatório de despesas do dia.
     */
    @Query("SELECT * FROM talao WHERE data = :data ORDER BY hora ASC")
    fun observarPorData(data: String): Flow<List<TalaoEntity>>

    /**
     * Funcionários únicos presentes num dia (deduplica por funcn).
     * Usado no relatório de presenças do dia.
     */
    @Query("""
        SELECT funcn, fref, nmfref, agnome, COUNT(*) as totalTaloes
        FROM talao
        WHERE data = :data AND funcn != ''
        GROUP BY funcn
        ORDER BY funcn ASC
    """)
    suspend fun obterPresencasDia(data: String): List<PresencaRow>

    /**
     * Total de despesas e contagem de talões por dia — usado nos indicadores do calendário.
     */
    @Query("""
        SELECT data, COUNT(*) as totalTaloes, SUM(total) as totalDespesas
        FROM talao
        WHERE data IS NOT NULL
        GROUP BY data
    """)
    fun observarResumoPorDia(): Flow<List<DiaResumoRow>>

    /**
     * Observa os N talões mais recentes, ordenados por data de criação.
     * Usado no ecrã Home para mostrar atividade recente.
     */
    @Query("SELECT * FROM talao ORDER BY criadoEm DESC LIMIT :limite")
    fun observarUltimos(limite: Int): Flow<List<TalaoEntity>>

    /**
     * Contagem de talões para um dia específico (não-reativa).
     * Usado pelo LembreteWorker para verificar se há talões registados hoje.
     */
    @Query("SELECT COUNT(*) FROM talao WHERE data = :data")
    suspend fun contarPorData(data: String): Int
}

/** Resultado agregado de presenças num dia. */
data class PresencaRow(
    val funcn: String,
    val fref: String,
    val nmfref: String,
    val agnome: String,
    val totalTaloes: Int
)

/** Resumo por dia para indicadores do calendário. */
data class DiaResumoRow(
    val data: String,
    val totalTaloes: Int,
    val totalDespesas: Double?
)
