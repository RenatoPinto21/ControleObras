package pt.controleobras.app.core.model

/**
 * Centro de custo / obra, carregado da tabela FREF do MariaDB.
 *
 * @param fref   Código do centro de custo (ex: "25202")
 * @param nmfref Nome da obra (ex: "Mercadona Amarante - CC e IH")
 * @param agnome Nome do encarregado responsável (ex: "Jorge Pereira")
 */
data class CentroCusto(
    val fref: String,
    val nmfref: String,
    val agnome: String
) {
    /** Texto exibido no dropdown: "25202 — Mercadona Amarante - CC e IH" */
    val labelDropdown: String get() = "$fref — $nmfref"
}
