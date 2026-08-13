package pt.controleobras.app.core.database.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pt.controleobras.app.core.database.dao.SubFuncDao
import pt.controleobras.app.core.database.entity.SubFuncEntity
import pt.controleobras.app.core.model.Funcionario
import pt.controleobras.app.core.relatorios.model.LinhaPresencaReg
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório de Funcionários (SUBFUNC) e Registos de Presença (SUBFUNC_REG).
 *
 * Fluxo:
 *   1. [sincronizarFuncionarios] — carrega funcionários de um CC do MariaDB → Room.
 *   2. [listarPorFref] — Flow offline do Room.
 *   3. [registarPresencas] — INSERT no SUBFUNC_REG do MariaDB.
 */
@Singleton
class SubFuncRepository @Inject constructor(
    private val remoteDb: RemoteDatabaseManager,
    private val dao: SubFuncDao
) {
    companion object {
        private const val TAG = "SubFuncRepo"

        private const val SQL_FUNCIONARIOS = """
            SELECT FREF, NMFREF, NOME, DESIGN, U_BISTAMPI, BISTAMP
            FROM PHPRetailConcept.SUBFUNC
            WHERE FREF = ?
            ORDER BY NOME ASC
        """

        private const val SQL_INSERT_REG = """
            INSERT INTO PHPRetailConcept.SUBFUNC_REG
                (ENCSERIE, REGSTAMP, U_BISTAMPI, BISTAMP, DATA, HORA, OBS,
                 DATAREG, HORAREG, OUSRINIS)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        /** Verifica se já existe registo para um BISTAMP num determinado dia. */
        private const val SQL_EXISTE_REG = """
            SELECT COUNT(*) FROM PHPRetailConcept.SUBFUNC_REG
            WHERE BISTAMP = ? AND DATE(DATA) = ?
        """

        /** Consulta presenças registadas por dia — todos os CC. */
        private const val SQL_CONSULTAR_REG = """
            SELECT r.REGSTAMP, r.HORA, r.OBS, r.BISTAMP,
                   r.DATAREG, r.HORAREG,
                   s.NOME, s.DESIGN, s.FREF, s.NMFREF
            FROM PHPRetailConcept.SUBFUNC_REG r
            LEFT JOIN PHPRetailConcept.SUBFUNC s ON r.BISTAMP = s.BISTAMP
            WHERE DATE(r.DATA) = ?
            ORDER BY s.NOME ASC
        """

        /** Consulta presenças registadas por dia — filtrado por CC. */
        private const val SQL_CONSULTAR_REG_CC = """
            SELECT r.REGSTAMP, r.HORA, r.OBS, r.BISTAMP,
                   r.DATAREG, r.HORAREG,
                   s.NOME, s.DESIGN, s.FREF, s.NMFREF
            FROM PHPRetailConcept.SUBFUNC_REG r
            LEFT JOIN PHPRetailConcept.SUBFUNC s ON r.BISTAMP = s.BISTAMP
            WHERE DATE(r.DATA) = ? AND s.FREF = ?
            ORDER BY s.NOME ASC
        """
    }

    /** Lista funcionários em cache (Room) para um centro de custo. */
    fun listarPorFref(fref: String): Flow<List<Funcionario>> =
        dao.listarPorFref(fref).map { lista -> lista.map { it.toDomain() } }

    /**
     * Sincroniza funcionários de um centro de custo com o MariaDB.
     *
     * @param fref Código do centro de custo.
     * @return [ResultadoSync] com estado e contagem.
     */
    suspend fun sincronizarFuncionarios(fref: String): ResultadoSync =
        withContext(Dispatchers.IO) {
            runCatching {
                remoteDb.obterLigacao().use { conn ->
                    conn.prepareStatement(SQL_FUNCIONARIOS.trimIndent()).use { stmt ->
                        stmt.setString(1, fref)
                        Log.d(TAG, "A consultar SUBFUNC para FREF='$fref'")
                        val rs    = stmt.executeQuery()
                        val lista = mutableListOf<SubFuncEntity>()
                        while (rs.next()) {
                            // IMPORTANTE: trim() em todos os campos — MariaDB CHAR
                            // devolve valores com padding de espaços que depois não
                            // coincidem nas queries SQLite (comparação exata).
                            val entity = SubFuncEntity(
                                fref       = rs.getString("FREF")?.trim()       ?: continue,
                                nmfref     = rs.getString("NMFREF")?.trim()     ?: "",
                                nome       = rs.getString("NOME")?.trim()       ?: "",
                                designacao = rs.getString("DESIGN")?.trim()     ?: "",
                                uBistampi  = rs.getString("U_BISTAMPI")?.trim() ?: "",
                                bistamp    = rs.getString("BISTAMP")?.trim()    ?: ""
                            )
                            lista += entity
                            Log.d(TAG, "  Func: ${entity.nome} — ${entity.designacao}")
                        }
                        rs.close()
                        dao.substituirPorFref(fref, lista)
                        Log.d(TAG, "Sync OK — ${lista.size} funcionários para CC $fref")
                        ResultadoSync.Sucesso(lista.size)
                    }
                }
            }.getOrElse { erro ->
                Log.e(TAG, "Falha sync funcionários: ${erro.javaClass.simpleName}")
                ResultadoSync.Erro("${erro.javaClass.simpleName}: ${erro.message ?: "Erro desconhecido"}")
            }
        }

    /**
     * Consulta presenças registadas no SUBFUNC_REG para um dia.
     *
     * @param data Data no formato AAAA-MM-DD.
     * @param fref Centro de custo (null = todos).
     * @return Lista de presenças com dados do funcionário (JOIN SUBFUNC).
     */
    suspend fun consultarPresencas(data: String, fref: String? = null): List<LinhaPresencaReg> =
        withContext(Dispatchers.IO) {
            runCatching {
                remoteDb.obterLigacao().use { conn ->
                    val sql  = if (fref.isNullOrBlank()) SQL_CONSULTAR_REG else SQL_CONSULTAR_REG_CC
                    conn.prepareStatement(sql.trimIndent()).use { stmt ->
                        stmt.setString(1, data)
                        if (!fref.isNullOrBlank()) stmt.setString(2, fref)
                        val rs    = stmt.executeQuery()
                        val lista = mutableListOf<LinhaPresencaReg>()
                        while (rs.next()) {
                            lista += LinhaPresencaReg(
                                nome       = rs.getString("NOME")?.trim()   ?: "—",
                                designacao = rs.getString("DESIGN")?.trim() ?: "",
                                fref       = rs.getString("FREF")?.trim()   ?: "",
                                nmfref     = rs.getString("NMFREF")?.trim() ?: "",
                                hora       = rs.getString("HORA")?.trim()   ?: "",
                                obs        = rs.getString("OBS")?.trim()    ?: "",
                                bistamp    = rs.getString("BISTAMP")?.trim() ?: "",
                                regstamp   = rs.getString("REGSTAMP")?.trim() ?: "",
                                dataReg    = rs.getString("DATAREG")?.trim() ?: "",
                                horaReg    = rs.getString("HORAREG")?.trim() ?: ""
                            )
                        }
                        rs.close()
                        Log.d(TAG, "Consulta presenças $data (CC=${fref ?: "todos"}) — ${lista.size} registos")
                        lista
                    }
                }
            }.getOrElse { erro ->
                Log.e(TAG, "Falha consulta presenças: ${erro.javaClass.simpleName}: ${erro.message}")
                emptyList()
            }
        }

    /**
     * Regista presenças no SUBFUNC_REG do MariaDB.
     *
     * REGSTAMP: identificador do lote — formato yyyyMMddHHmmss, mesmo para
     *           todos os funcionários registados no mesmo evento.
     * DATAREG/HORAREG: data e hora exata do momento do registo.
     * OUSRINIS: origem do registo — serial do dispositivo (max 30 chars).
     *
     * Funcionários já registados no mesmo dia são ignorados (sem duplicados).
     *
     * @param encserie  Serial do dispositivo.
     * @param presencas Lista de funcionários presentes.
     * @param data      Data da presença (formato AAAA-MM-DD).
     * @param hora      Hora do registo (formato HH:mm).
     * @param obs            Observações globais (opcional).
     * @param obsPorBistamp  Observações por funcionário (BISTAMP → obs).
     *                       Prioridade sobre [obs] quando presente e não vazio.
     * @return [ResultadoSync] com estado e contagem de novos registos.
     */
    suspend fun registarPresencas(
        encserie: String,
        presencas: List<Funcionario>,
        data: String,
        hora: String,
        obs: String,
        obsPorBistamp: Map<String, String> = emptyMap()
    ): ResultadoSync = withContext(Dispatchers.IO) {
        runCatching {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            val agora     = java.time.LocalDateTime.now()
            val regstamp  = agora.format(formatter)
            // DATAREG e HORAREG derivados do REGSTAMP (mesma origem, sem divergência)
            val regstampDt = java.time.LocalDateTime.parse(regstamp, formatter)
            val fmtTs      = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val fmtTm      = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
            val dataReg    = java.sql.Timestamp.valueOf(regstampDt.format(fmtTs))
            val horaReg    = java.sql.Time.valueOf(regstampDt.format(fmtTm))
            val dataSQL  = java.sql.Date.valueOf(data)
            val horaSQL  = java.sql.Time.valueOf("$hora:00")
            val ousrinis = encserie.take(30) // varchar(30) — evita truncatura

            remoteDb.obterLigacao().use { conn ->
                // 1. Verificar duplicados — ignorar funcionários já registados neste dia
                val duplicados = mutableSetOf<String>()
                conn.prepareStatement(SQL_EXISTE_REG.trimIndent()).use { stmtCheck ->
                    for (func in presencas) {
                        stmtCheck.setString(1, func.bistamp)
                        stmtCheck.setString(2, data)
                        val rs = stmtCheck.executeQuery()
                        if (rs.next() && rs.getInt(1) > 0) {
                            duplicados += func.bistamp
                            Log.d(TAG, "Já registado: ${func.nome} (${func.bistamp}) em $data")
                        }
                        rs.close()
                    }
                }

                val novos = presencas.filter { it.bistamp !in duplicados }
                if (novos.isEmpty()) {
                    Log.d(TAG, "Todos os funcionários já estavam registados em $data")
                    return@withContext ResultadoSync.Sucesso(0)
                }

                // 2. Inserir apenas os novos
                conn.prepareStatement(SQL_INSERT_REG.trimIndent()).use { stmt ->
                    for (func in novos) {
                        stmt.setString(1, encserie)       // ENCSERIE
                        stmt.setString(2, regstamp)        // REGSTAMP (mesmo para todo o lote)
                        stmt.setString(3, func.uBistampi)  // U_BISTAMPI
                        stmt.setString(4, func.bistamp)    // BISTAMP
                        stmt.setDate(5, dataSQL)            // DATA (datetime)
                        stmt.setTime(6, horaSQL)            // HORA (time)
                        val obsFunc = obsPorBistamp[func.bistamp]
                            ?.takeIf { it.isNotBlank() } ?: obs
                        stmt.setString(7, obsFunc)           // OBS
                        stmt.setTimestamp(8, dataReg)        // DATAREG
                        stmt.setTime(9, horaReg)             // HORAREG
                        stmt.setString(10, ousrinis)         // OUSRINIS
                        stmt.addBatch()
                    }
                    val resultados = stmt.executeBatch()
                    val total = resultados.count { it >= 0 }
                    Log.d(TAG, "Registadas $total presenças para $data (${duplicados.size} duplicados ignorados)")
                    ResultadoSync.Sucesso(total)
                }
            }
        }.getOrElse { erro ->
            Log.e(TAG, "Falha ao registar presenças: ${erro.javaClass.simpleName}")
            ResultadoSync.Erro("${erro.javaClass.simpleName}: ${erro.message ?: "Erro desconhecido"}")
        }
    }
}
