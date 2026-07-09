package pt.controleobras.app.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: adiciona a coluna [pt.controleobras.app.core.database.entity.TalaoEntity.textoOcr]
 * (texto bruto do OCR, para nunca se perder informação mesmo quando o parser falha).
 * Não apaga dados existentes.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE talao ADD COLUMN textoOcr TEXT")
    }
}
