package pt.controleobras.app.core.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Resultado do OCR + parser, antes da confirmação do utilizador.
 * Todos os campos textuais para permitir edição livre no formulário de revisão;
 * a validação/conversão para tipos fortes (BigDecimal, etc.) só acontece ao confirmar.
 */
data class TalaoDraft(
    val empresa: String = "",
    val nif: String = "",
    val morada: String = "",
    val data: LocalDate? = null,
    val hora: LocalTime? = null,
    val numeroFatura: String = "",
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
    val total: String = ""
)
