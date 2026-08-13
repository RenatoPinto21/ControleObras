package pt.controleobras.app.core.relatorios.model

import java.time.LocalDate

/**
 * Resumo de um dia para os indicadores visuais do calendário.
 *
 * @param data           O dia em questão
 * @param totalTaloes    Número de talões registados nesse dia
 * @param totalDespesas  Soma dos totais dos talões (pode ser null se não houver total em nenhum)
 * @param totalPresencas Número de funcionários únicos presentes (funcn distintos)
 */
data class DiaResumo(
    val data: LocalDate,
    val totalTaloes: Int,
    val totalDespesas: Double?,
    val totalPresencas: Int
) {
    val temDespesas: Boolean get() = totalTaloes > 0
    val temPresencas: Boolean get() = totalPresencas > 0
}
