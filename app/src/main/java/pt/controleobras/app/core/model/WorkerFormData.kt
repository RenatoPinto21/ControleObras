package pt.controleobras.app.core.model

/**
 * Dados do formulário preenchido pelo funcionário antes de fotografar o talão.
 * Campos obrigatórios — a app não avança sem eles preenchidos.
 *
 * @param funcn     Nº do funcionário (FUNCN)
 * @param ccnome    Centro de custo / descrição (FUNCDESC no CSV)
 * @param funobs    Observações do funcionário (FUNOBS)
 */
data class WorkerFormData(
    val funcn: String,
    val ccnome: String,
    val funobs: String
)
