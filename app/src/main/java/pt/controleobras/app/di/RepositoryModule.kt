package pt.controleobras.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pt.controleobras.app.data.repository.TalaoRepository
import pt.controleobras.app.data.repository.TalaoRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTalaoRepository(impl: TalaoRepositoryImpl): TalaoRepository
}
