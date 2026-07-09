package pt.controleobras.app.core.parser

import pt.controleobras.app.core.model.TalaoDraft

/**
 * Interpreta o texto bruto devolvido pelo OCR e extrai os campos estruturados
 * de um talão. Campos não identificados ficam vazios/nulos para confirmação manual.
 */
interface ReceiptParser {
    fun parse(textoReconhecido: String, imagemPath: String): TalaoDraft
}
