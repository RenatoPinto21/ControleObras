package pt.controleobras.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import pt.controleobras.app.core.database.converter.Converters
import pt.controleobras.app.core.database.dao.TalaoDao
import pt.controleobras.app.core.database.entity.TalaoEntity

@Database(entities = [TalaoEntity::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun talaoDao(): TalaoDao
}
