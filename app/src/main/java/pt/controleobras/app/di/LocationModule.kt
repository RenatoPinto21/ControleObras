package pt.controleobras.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pt.controleobras.app.core.location.FusedLocationProvider
import pt.controleobras.app.core.location.LocationProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: FusedLocationProvider): LocationProvider
}
