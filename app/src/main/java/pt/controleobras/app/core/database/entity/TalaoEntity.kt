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
    val nifCliente: String? = null,
    val morada: String?,
    val data: LocalDate?,
    val hora: LocalTime?,
    val serie: String? = null,
    val numeroFatura: String?,
    val dataVencimento: LocalDate? = null,
    val metodoPagamento: String? = null,
    val itens: List<ItemTalaoDto>,
    val iva: BigDecimal?,
    val total: BigDecimal?,
    val observacoes: String?,
    val imagemPath: String,
    val criadoEm: Instant,
    val textoOcr: String? = null
)
