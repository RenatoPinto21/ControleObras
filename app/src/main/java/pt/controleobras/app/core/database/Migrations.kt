package pt.controleobras.app.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: adiciona [textoOcr] — texto bruto do OCR.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE talao ADD COLUMN textoOcr TEXT")
    }
}

/**
 * v2 → v3: adiciona campos de fatura detalhados extraídos pelo LLM/parser.
 *
 * Novos campos:
 *  - nifCliente     — NIF do cliente (empresa de construção)
 *  - serie          — Série do documento AT (ex: "A" de "FT A/1234")
 *  - dataVencimento — Data de vencimento do pagamento (armazenada como TEXT via TypeConverter)
 *  - metodoPagamento — Método de pagamento (ex: "Multibanco", "MB Way")
 *
 * Os campos dos itens (desconto, taxaIva) não precisam de migração SQL
 * porque são serializados como JSON dentro da coluna 'itens' (TypeConverter).
 * O valor por omissão "" em [ItemTalaoDto] garante compatibilidade com registos antigos.
 *
 * Não apaga dados existentes.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE talao ADD COLUMN nifCliente TEXT")
        db.execSQL("ALTER TABLE talao ADD COLUMN serie TEXT")
        db.execSQL("ALTER TABLE talao ADD COLUMN dataVencimento TEXT")
        db.execSQL("ALTER TABLE talao ADD COLUMN metodoPagamento TEXT")
    }
}
