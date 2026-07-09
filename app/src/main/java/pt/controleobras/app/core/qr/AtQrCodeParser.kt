package pt.controleobras.app.core.qr

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Interpreta o QR code normalizado pela Autoridade Tributária (AT) portuguesa,
 * obrigatório em faturas emitidas desde 1 de julho de 2021.
 *
 * Formato: campos separados por '*', cada campo com a forma "CHAVE:VALOR".
 * Exemplo mínimo:
 *   A:508018085*B:999999990*C:PT*D:FT*E:N*F:20211231*G:FT A/1*N:23.00*O:123.00*Q:abcd*R:1234
 *
 * Campos extraídos:
 *   A  = NIF do emitente (vendedor) → [nif]
 *   D  = Tipo de documento (FT, FS, FR, NC, ND, ...) — usado para validar que é fatura
 *   F  = Data no formato YYYYMMDD → [data]
 *   G  = Número do documento (ex: "FT A/1") → [numeroFatura]
 *   N  = Total de impostos (IVA) → [iva]
 *   O  = Total com impostos (valor a pagar) → [total]
 *
 * Campos não presentes no QR AT (têm de vir do OCR):
 *   empresa, morada, hora, produtos, observações
 *
 * Referência: Portaria n.º 195/2020, Anexo I, ponto 2.
 */
class AtQrCodeParser @Inject constructor() {

    /**
     * Tenta interpretar [qrContent] como um QR code AT.
     * Devolve [AtQrData] com os campos encontrados, ou null se o conteúdo
     * não parecer um QR code AT (ausência do campo obrigatório 'A').
     */
    fun parse(qrContent: String): AtQrData? {
        val campos = qrContent
            .split("*")
            .mapNotNull { parte ->
                val idx = parte.indexOf(':')
                if (idx < 1) null
                else parte.substring(0, idx).trim() to parte.substring(idx + 1).trim()
            }
            .toMap()

        // Campo 'A' (NIF emitente) é obrigatório no formato AT
        val nifEmitente = campos["A"] ?: return null
        if (!nifEmitente.all(Char::isDigit) || nifEmitente.length !in 9..10) return null

        return AtQrData(
            nif = nifEmitente,
            tipoDocumento = campos["D"],
            data = parseData(campos["F"]),
            numeroFatura = campos["G"],
            totalIva = campos["N"]?.replace(',', '.'),
            totalComIva = campos["O"]?.replace(',', '.')
        )
    }

    private fun parseData(valor: String?): LocalDate? {
        valor ?: return null
        return runCatching {
            LocalDate.parse(valor, DateTimeFormatter.BASIC_ISO_DATE) // YYYYMMDD
        }.getOrNull()
    }
}

/**
 * Dados extraídos do QR code AT.
 * Todos os campos são nullable — o parser extrai apenas o que encontrar.
 */
data class AtQrData(
    /** NIF do emitente (vendedor). */
    val nif: String,
    /** Tipo de documento: FT=Fatura, FS=Fatura Simplificada, FR=Fatura-Recibo, NC=Nota Crédito, etc. */
    val tipoDocumento: String?,
    /** Data do documento. */
    val data: LocalDate?,
    /** Número do documento, ex: "FT A/1". */
    val numeroFatura: String?,
    /** Total de IVA (campo N). */
    val totalIva: String?,
    /** Total com IVA / valor a pagar (campo O). */
    val totalComIva: String?
)
