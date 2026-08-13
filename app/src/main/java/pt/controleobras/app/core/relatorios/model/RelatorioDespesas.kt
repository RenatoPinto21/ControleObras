package pt.controleobras.app.core.relatorios.model

import java.time.LocalDate

/**
 * Relatório de despesas para um dia específico.
 *
 * @param data       Dia do relatório
 * @param linhas     Lista de talões registados nesse dia
 * @param totalGeral Soma de todos os totais
 */
data class RelatorioDespesas(
    val data: LocalDate,
    val linhas: List<LinhaDespesa>,
    val totalGeral: Double
)

data class LinhaDespesa(
    val id: Long,
    val hora: String,
    val empresa: String,
    val nif: String,
    val numeroFatura: String,
    val total: Double,
    val funcn: String,
    val fref: String,
    val nmfref: String
)
