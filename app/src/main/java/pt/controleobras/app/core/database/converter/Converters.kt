package pt.controleobras.app.core.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pt.controleobras.app.core.database.entity.ItemTalaoDto
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Conversores Room para tipos que a base de dados não suporta nativamente.
 * Datas/horas em ISO-8601, valores monetários como texto (evita perda de precisão do BigDecimal),
 * lista de itens serializada em JSON.
 */
class Converters {

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun fromInstant(value: Instant?): String? = value?.toString()

    @TypeConverter
    fun toInstant(value: String?): Instant? = value?.let(Instant::parse)

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)

    @TypeConverter
    fun fromItens(value: List<ItemTalaoDto>): String = Json.encodeToString(value)

    @TypeConverter
    fun toItens(value: String): List<ItemTalaoDto> = Json.decodeFromString(value)
}
