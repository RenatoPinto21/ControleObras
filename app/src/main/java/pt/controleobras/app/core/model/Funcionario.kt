package pt.controleobras.app.core.model

/**
 * Funcionário associado a um centro de custo, carregado da tabela SUBFUNC do MariaDB.
 *
 * @param fref       Código do centro de custo
 * @param nmfref     Nome da obra
 * @param nome       Nome do funcionário
 * @param designacao Função/designação do funcionário
 * @param uBistampi  Identificador interno (chave de ligação)
 * @param bistamp    Identificador interno (chave de ligação)
 */
data class Funcionario(
    val fref: String,
    val nmfref: String,
    val nome: String,
    val designacao: String,
    val uBistampi: String,
    val bistamp: String
)
