package pt.controleobras.app.core.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Talão confirmado pelo utilizador e pronto a ser persistido.
 * Campos que não são obrigatoriamente extraídos pelo OCR são nullable.
 */
data class Talao(
    val id: Long = 0,

    /** Nome da empresa emitente (fornecedor). */
    val empresa: String,

    /** NIF do fornecedor. */
    val nif: String?,

    /** NIF do cliente — empresa de construção. */
    val nifCliente: String? = null,

    /** Morada do fornecedor. */
    val morada: String?,

    /** Data de emissão. */
    val data: LocalDate?,

    val hora: LocalTime?,

    /** Série do documento AT (ex: "A"). */
    val serie: String? = null,

    /** Número completo do documento (ex: "FT A/1234"). */
    val numeroFatura: String?,

    /** Data de vencimento do pagamento. */
    val dataVencimento: LocalDate? = null,

    /** Método de pagamento (ex: "Multibanco", "MB Way", "Numerário"). */
    val metodoPagamento: String? = null,

    val itens: List<ItemTalao>,
    val iva: BigDecimal?,
    val total: BigDecimal?,
    val observacoes: String?,
    val imagemPath: String,
    val criadoEm: Instant = Instant.now(),

    /** Texto bruto do OCR — nunca se perde, mesmo quando o parser falha. */
    val textoOcr: String? = null,

    // Dados do funcionário — preenchidos no WorkerFormScreen antes da captura
    val funcn: String = "",
    val fref: String = "",
    val nmfref: String = "",
    val agnome: String = ""
)
