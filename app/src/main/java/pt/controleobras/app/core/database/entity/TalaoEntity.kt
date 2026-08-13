package pt.controleobras.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Entidade principal da base de dados — representa um talão ou fatura digitalizada.
 *
 * Cada registo corresponde a uma fatura que o utilizador fotografou na obra.
 * Os campos são preenchidos automaticamente pelo OCR e validados pelo utilizador.
 *
 * A tabela "talao" é o coração da aplicação — todos os relatórios,
 * listagens e exportações dependem destes dados.
 *
 * Os campos nullable (String?, LocalDate?, etc.) significam que o OCR
 * não conseguiu extrair esse dado ou que o campo não existe na fatura.
 *
 * Os campos funcn/fref/nmfref/agnome foram adicionados na migração v4→v5
 * para suportar relatórios de presenças por funcionário e centro de custo.
 */
@Entity(tableName = "talao")
data class TalaoEntity(
    /** ID único gerado automaticamente pelo Room (auto-increment). */
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Nome da empresa emissora da fatura (ex: "Leroy Merlin"). */
    val empresa: String,
    /** NIF do fornecedor/emitente (9 dígitos). Extraído do QR AT ou OCR. */
    val nif: String?,
    /** NIF do cliente (quem comprou). Pode vir do QR code AT. */
    val nifCliente: String? = null,
    /** Morada do emitente, extraída por OCR. */
    val morada: String?,
    /** Data de emissão da fatura. */
    val data: LocalDate?,
    /** Hora de emissão da fatura. */
    val hora: LocalTime?,
    /** Série do documento (ex: "FT A/2024"). */
    val serie: String? = null,
    /** Número da fatura (ex: "FT 123/2024"). */
    val numeroFatura: String?,
    /** Data de vencimento (se aplicável — nem todas as faturas têm). */
    val dataVencimento: LocalDate? = null,
    /** Método de pagamento (ex: "Multibanco", "Numerário"). */
    val metodoPagamento: String? = null,
    /** Lista de itens/linhas da fatura (descrição, quantidade, preço, IVA). */
    val itens: List<ItemTalaoDto>,
    /** Total de IVA da fatura. */
    val iva: BigDecimal?,
    /** Valor total da fatura (com IVA incluído). */
    val total: BigDecimal?,
    /** Observações livres introduzidas pelo utilizador no formulário. */
    val observacoes: String?,
    /** Caminho absoluto para a imagem original da fatura no disco do dispositivo. */
    val imagemPath: String,
    /** Data/hora exata em que o registo foi criado na app (timestamp UTC). */
    val criadoEm: Instant,
    /** Texto completo reconhecido pelo OCR — útil para pesquisa e debug. */
    val textoOcr: String? = null,
    // ── Dados do funcionário (adicionados na migração v5) ────────────────
    /** Número do funcionário (campo obrigatório no formulário). */
    val funcn: String = "",
    /** Código do centro de custo / obra (ex: "FREF001"). */
    val fref: String = "",
    /** Nome do centro de custo / obra (ex: "Obra Rua Augusta"). */
    val nmfref: String = "",
    /** Abreviatura / agnome do centro de custo. */
    val agnome: String = ""
)
