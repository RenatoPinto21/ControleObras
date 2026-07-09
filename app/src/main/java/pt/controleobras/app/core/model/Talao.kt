package pt.controleobras.app.core.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Talão confirmado pelo utilizador e pronto a ser persistido.
 * Campos que não são obrigatoriamente extraídos pelo OCR (NIF, morada, etc.)
 * são nullable — o utilizador pode deixá-los em branco na confirmação.
 */
data class Talao(
    val id: Long = 0,
    val empresa: String,
    val nif: String?,
    val morada: String?,
    val data: LocalDate?,
    val hora: LocalTime?,
    val numeroFatura: String?,
    val itens: List<ItemTalao>,
    val iva: BigDecimal?,
    val total: BigDecimal?,
    val observacoes: String?,
    val imagemPath: String,
    val criadoEm: Instant = Instant.now(),
    /** Texto bruto devolvido pelo OCR — preservado sempre, mesmo que o parser não tenha conseguido interpretar tudo. */
    val textoOcr: String? = null
)
