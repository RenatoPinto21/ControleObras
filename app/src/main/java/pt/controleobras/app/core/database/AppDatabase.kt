package pt.controleobras.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import pt.controleobras.app.core.database.converter.Converters
import pt.controleobras.app.core.database.dao.CentroCustoDao
import pt.controleobras.app.core.database.dao.SubFuncDao
import pt.controleobras.app.core.database.dao.TalaoDao
import pt.controleobras.app.core.database.entity.CentroCustoEntity
import pt.controleobras.app.core.database.entity.SubFuncEntity
import pt.controleobras.app.core.database.entity.TalaoEntity

@Database(
    entities  = [TalaoEntity::class, CentroCustoEntity::class, SubFuncEntity::class],
    version   = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun talaoDao(): TalaoDao
    abstract fun centroCustoDao(): CentroCustoDao
    abstract fun subFuncDao(): SubFuncDao
}
