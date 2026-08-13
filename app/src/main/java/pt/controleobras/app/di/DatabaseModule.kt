package pt.controleobras.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pt.controleobras.app.core.database.AppDatabase
import pt.controleobras.app.core.database.MIGRATION_1_2
import pt.controleobras.app.core.database.MIGRATION_2_3
import pt.controleobras.app.core.database.MIGRATION_3_4
import pt.controleobras.app.core.database.MIGRATION_4_5
import pt.controleobras.app.core.database.MIGRATION_5_6
import pt.controleobras.app.core.database.dao.CentroCustoDao
import pt.controleobras.app.core.database.dao.SubFuncDao
import pt.controleobras.app.core.database.dao.TalaoDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "controle_obras.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()

    @Provides
    fun provideTalaoDao(database: AppDatabase): TalaoDao = database.talaoDao()

    @Provides
    fun provideCentroCustoDao(database: AppDatabase): CentroCustoDao = database.centroCustoDao()

    @Provides
    fun provideSubFuncDao(database: AppDatabase): SubFuncDao = database.subFuncDao()
}
