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

/**
 * v3 → v4: cria tabela [CentroCustoEntity] para cache local da tabela FREF do MariaDB.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS centro_custo (
                fref   TEXT NOT NULL PRIMARY KEY,
                nmfref TEXT NOT NULL,
                agnome TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

/**
 * v4 → v5: adiciona campos do WorkerFormData ao [TalaoEntity]
 * para suporte a relatórios de despesas e presenças por dia.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE talao ADD COLUMN funcn  TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE talao ADD COLUMN fref   TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE talao ADD COLUMN nmfref TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE talao ADD COLUMN agnome TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * v5 → v6: cria tabela [SubFuncEntity] para cache local da tabela SUBFUNC do MariaDB.
 * Guarda funcionários associados a cada centro de custo.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS subfunc (
                fref       TEXT NOT NULL,
                nmfref     TEXT NOT NULL,
                nome       TEXT NOT NULL,
                designacao TEXT NOT NULL,
                u_bistampi TEXT NOT NULL,
                bistamp    TEXT NOT NULL,
                PRIMARY KEY(bistamp)
            )
            """.trimIndent()
        )
    }
}
