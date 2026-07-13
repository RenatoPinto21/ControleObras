package pt.controleobras.app.core.validation

/**
 * Estado de validação de um campo extraído da fatura.
 *
 *  VALID   — campo encontrado e passou em todas as regras de validação.
 *  SUSPECT — campo encontrado mas falhou numa regra (ex: NIF com checksum errado).
 *            O valor é apresentado ao utilizador com aviso para confirmar.
 *  MISSING — campo não encontrado na imagem.
 *            O utilizador deve verificar na imagem original.
 */
enum class FieldState { VALID, SUSPECT, MISSING }

/**
 * Resultado da validação de um campo individual.
 *
 * @param state  Estado resultante da validação.
 * @param hint   Mensagem de ajuda para o utilizador (opcional, usada em SUSPECT e MISSING).
 */
data class FieldValidation(
    val state: FieldState,
    val hint: String? = null
) {
    companion object {
        fun valid()                   = FieldValidation(FieldState.VALID)
        fun suspect(hint: String)     = FieldValidation(FieldState.SUSPECT, hint)
        fun missing(hint: String? = null) = FieldValidation(FieldState.MISSING, hint)
    }
}
