package pt.controleobras.app.core.model

/**
 * Dados do formulário preenchido pelo funcionário antes de fotografar o talão.
 *
 * @param funcn        Nº do funcionário (FUNCN)
 * @param centroCusto  Centro de custo selecionado no dropdown (vem da BD FREF)
 * @param funobs       Observações do funcionário (FUNOBS)
 */
data class WorkerFormData(
    val funcn: String,
    val centroCusto: CentroCusto?,
    val funobs: String
)
