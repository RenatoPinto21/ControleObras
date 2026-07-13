package pt.controleobras.app.core.validation

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validador de NIF (Número de Identificação Fiscal) português.
 *
 * Regras aplicadas:
 *  1. Exactamente 9 dígitos
 *  2. Sem letras ou outros caracteres
 *  3. Primeiro dígito ∈ {1,2,3,5,6,7,8,9} — 0 e 4 são inválidos
 *  4. Checksum módulo 11 (algoritmo oficial AT)
 *
 * Referência: Decreto-Lei n.º 14/2013 e Portaria n.º 195/2020 (AT Portugal)
 */
@Singleton
class NifValidator @Inject constructor() {

    /**
     * Verifica se [nif] é um NIF português válido.
     * Devolve true apenas se todas as 4 regras passarem.
     */
    fun isValid(nif: String): Boolean {
        val limpo = nif.trim()
        if (limpo.length != 9) return false
        if (!limpo.all(Char::isDigit)) return false
        val primeiro = limpo[0] - '0'
        if (primeiro == 0 || primeiro == 4) return false
        return checksumValido(limpo)
    }

    /**
     * Razão pela qual o NIF é inválido, ou null se for válido.
     * Útil para mostrar mensagens específicas ao utilizador.
     */
    fun motivoInvalido(nif: String): String? {
        val limpo = nif.trim()
        if (limpo.isEmpty()) return null           // vazio → tratado como MISSING, não SUSPECT
        if (limpo.length != 9) return "NIF deve ter exactamente 9 dígitos (tem ${limpo.length})"
        if (!limpo.all(Char::isDigit)) return "NIF não pode conter letras ou símbolos"
        val primeiro = limpo[0] - '0'
        if (primeiro == 0 || primeiro == 4) return "NIF começa por $primeiro — primeiro dígito inválido"
        if (!checksumValido(limpo)) return "Dígito de controlo do NIF não é válido — verifique na fatura"
        return null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Algoritmo módulo 11
    // ─────────────────────────────────────────────────────────────────────────

    private fun checksumValido(nif: String): Boolean {
        val digitos = nif.map { it - '0' }
        // Pesos: 9,8,7,6,5,4,3,2 para os primeiros 8 dígitos
        val soma = digitos.take(8).mapIndexed { i, v -> v * (9 - i) }.sum()
        val resto = soma % 11
        val digitoControlo = if (resto < 2) 0 else 11 - resto
        return digitoControlo == digitos[8]
    }
}
