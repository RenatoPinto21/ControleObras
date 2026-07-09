package pt.controleobras.app.core.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON estruturado devolvido pelo modelo LLM depois de analisar o texto OCR.
 *
 * Os nomes dos campos estão em snake_case para coincidir com o que o modelo
 * devolve (o prompt usa snake_case). O [SerialName] faz a correspondência com
 * o campo Kotlin.
 *
 * Todos os campos são nullable — o modelo coloca null quando não encontra
 * o valor (instrução explícita no prompt: "Nunca inventes valores").
 */
@Serializable
data class LlmExtractionResult(
    /** Nome da empresa emitente (fornecedor). */
    val fornecedor: String? = null,

    /** NIF de 9 dígitos do fornecedor. */
    @SerialName("nif_fornecedor")
    val nifFornecedor: String? = null,

    /** NIF de 9 dígitos do cliente (empresa de construção). */
    @SerialName("nif_cliente")
    val nifCliente: String? = null,

    /** Morada do fornecedor. */
    val morada: String? = null,

    /** Número completo do documento (ex: "FT A/1234"). */
    @SerialName("numero_fatura")
    val numeroFatura: String? = null,

    /** Série do documento (ex: "A"). */
    val serie: String? = null,

    /** Data de emissão no formato dd/MM/yyyy ou yyyy-MM-dd. */
    @SerialName("data_emissao")
    val dataEmissao: String? = null,

    /** Data de vencimento no formato dd/MM/yyyy ou yyyy-MM-dd. */
    @SerialName("data_vencimento")
    val dataVencimento: String? = null,

    /** Hora no formato HH:mm. */
    val hora: String? = null,

    /** Total sem IVA (ex: "100.00"). */
    val subtotal: String? = null,

    /** Total de IVA (ex: "23.00"). */
    @SerialName("iva_total")
    val ivaTotal: String? = null,

    /** Total a pagar (ex: "123.00"). */
    val total: String? = null,

    /** Método de pagamento (ex: "Multibanco", "MB Way", "Numerário", "Visa"). */
    @SerialName("metodo_pagamento")
    val metodoPagamento: String? = null,

    /** Linhas de produto/serviço. */
    val linhas: List<LlmItemResult> = emptyList(),

    /** Observações ou notas adicionais. */
    val observacoes: String? = null
)

@Serializable
data class LlmItemResult(
    val descricao: String? = null,
    val quantidade: String? = null,

    @SerialName("preco_unitario")
    val precoUnitario: String? = null,

    /** Desconto na linha (valor ou percentagem). */
    val desconto: String? = null,

    /** Taxa de IVA em % (ex: "23", "13", "6"). */
    @SerialName("taxa_iva")
    val taxaIva: String? = null,

    @SerialName("total_linha")
    val totalLinha: String? = null
)
