package pt.controleobras.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import pt.controleobras.app.core.model.CentroCusto

/**
 * Cache local da tabela FREF do MariaDB.
 * Populada pelo [FrefRepository] a cada sincronização.
 */
@Entity(tableName = "centro_custo")
data class CentroCustoEntity(
    @PrimaryKey val fref: String,
    val nmfref: String,
    val agnome: String
) {
    fun toDomain() = CentroCusto(fref = fref, nmfref = nmfref, agnome = agnome)

    companion object {
        fun fromDomain(cc: CentroCusto) = CentroCustoEntity(
            fref   = cc.fref,
            nmfref = cc.nmfref,
            agnome = cc.agnome
        )
    }
}
