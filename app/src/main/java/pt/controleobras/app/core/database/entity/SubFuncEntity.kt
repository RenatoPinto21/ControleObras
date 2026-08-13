package pt.controleobras.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import pt.controleobras.app.core.model.Funcionario

/**
 * Cache local da tabela SUBFUNC do MariaDB.
 * Chave composta: BISTAMP é único por funcionário+CC.
 */
@Entity(
    tableName = "subfunc",
    primaryKeys = ["bistamp"]
)
data class SubFuncEntity(
    val fref: String,
    val nmfref: String,
    val nome: String,
    @ColumnInfo(name = "designacao")
    val designacao: String,
    @ColumnInfo(name = "u_bistampi")
    val uBistampi: String,
    val bistamp: String
) {
    fun toDomain(): Funcionario = Funcionario(
        fref       = fref.trim(),
        nmfref     = nmfref.trim(),
        nome       = nome.trim(),
        designacao = designacao.trim(),
        uBistampi  = uBistampi.trim(),
        bistamp    = bistamp.trim()
    )
}
