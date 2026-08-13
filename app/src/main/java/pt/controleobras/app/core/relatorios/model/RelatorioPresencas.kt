package pt.controleobras.app.core.relatorios.model

import java.time.LocalDate

/**
 * Relatório de presenças para um dia específico.
 *
 * @param data       Dia do relatório
 * @param linhas     Lista de funcionários presentes (deduplicados por funcn)
 */
data class RelatorioPresencas(
    val data: LocalDate,
    val linhas: List<LinhaPresenca>
)

data class LinhaPresenca(
    val funcn: String,
    val fref: String,
    val nmfref: String,
    val agnome: String,
    val totalTaloes: Int
)

/**
 * Relatório de presenças registadas via SUBFUNC_REG.
 *
 * @param data         Dia do relatório
 * @param linhas       Lista de funcionários presentes
 * @param frefFiltro   Centro de custo filtrado (null = todos)
 */
data class RelatorioPresencasReg(
    val data: LocalDate,
    val linhas: List<LinhaPresencaReg>,
    val frefFiltro: String? = null
)

/**
 * Linha de presença registada na tabela SUBFUNC_REG (com JOIN a SUBFUNC).
 *
 * @param nome       Nome do funcionário (SUBFUNC.NOME)
 * @param designacao Função do funcionário (SUBFUNC.DESIGN)
 * @param fref       Código do centro de custo (SUBFUNC.FREF)
 * @param nmfref     Nome da obra (SUBFUNC.NMFREF)
 * @param hora       Hora da presença (SUBFUNC_REG.HORA)
 * @param obs        Observações (SUBFUNC_REG.OBS)
 * @param bistamp    Identificador do funcionário (SUBFUNC_REG.BISTAMP)
 * @param regstamp   Identificador do lote de registo (SUBFUNC_REG.REGSTAMP)
 * @param dataReg    Data exata do registo (SUBFUNC_REG.DATAREG)
 * @param horaReg    Hora exata do registo (SUBFUNC_REG.HORAREG)
 */
data class LinhaPresencaReg(
    val nome: String,
    val designacao: String,
    val fref: String,
    val nmfref: String,
    val hora: String,
    val obs: String,
    val bistamp: String,
    val regstamp: String = "",
    val dataReg: String = "",
    val horaReg: String = ""
)
