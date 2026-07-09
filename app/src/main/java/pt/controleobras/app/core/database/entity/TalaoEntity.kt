package pt.controleobras.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "talao")
data class TalaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val empresa: String,
    val nif: String?,
    val morada: String?,
    val data: LocalDate?,
    val hora: LocalTime?,
    val numeroFatura: String?,
    val itens: List<ItemTalaoDto>,
    val iva: BigDecimal?,
    val total: BigDecimal?,
    val observacoes: String?,
    val imagemPath: String,
    val criadoEm: Instant,
    val textoOcr: String? = null
)
