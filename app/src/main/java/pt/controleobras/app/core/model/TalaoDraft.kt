package pt.controleobras.app.core.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Resultado do OCR + LLM/parser, antes da confirmação do utilizador.
 *
 * Todos os campos textuais para permitir edição livre no formulário de revisão;
 * a validação/conversão para tipos fortes (BigDecimal, LocalDate, etc.)
 * só acontece ao confirmar, em [paraDominio].
 */
data class TalaoDraft(
    /** Nome da empresa emitente (fornecedor). */
    val empresa: String = "",

    /** NIF do fornecedor (9 dígitos). */
    val nif: String = "",

    /** NIF do cliente — empresa de construção a quem a fatura foi emitida. */
    val nifCliente: String = "",

    /** Morada do fornecedor. */
    val morada: String = "",

    /** Data de emissão da fatura. */
    val data: LocalDate? = null,

    val hora: LocalTime? = null,

    /**
     * Série do documento AT (ex: "A" de "FT A/1234").
     * Separado do [numeroFatura] para facilitar pesquisas e exportações.
     */
    val serie: String = "",

    /** Número completo do documento (ex: "FT A/1234"). */
    val numeroFatura: String = "",

    /** Data limite de pagamento (se indicada na fatura). */
    val dataVencimento: LocalDate? = null,

    /** Método de pagamento (ex: "Multibanco", "MB Way", "Numerário", "Visa"). */
    val metodoPagamento: String = "",

    val itens: List<ItemTalaoDraft> = emptyList(),
    val iva: String = "",
    val total: String = "",
    val observacoes: String = "",

    val imagemPath: String,
    val textoReconhecido: String = ""
)

data class ItemTalaoDraft(
    val descricao: String = "",
    val quantidade: String = "",
    val precoUnitario: String = "",

    /** Desconto aplicado na linha (valor ou percentagem — tal como aparece na fatura). */
    val desconto: String = "",

    /** Taxa de IVA da linha em % (ex: "23", "13", "6"). */
    val taxaIva: String = "",

    val total: String = ""
)
