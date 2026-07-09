package pt.controleobras.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pt.controleobras.app.core.drive.DriveUploader
import pt.controleobras.app.core.drive.SafDriveUploader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DriveModule {

    @Binds
    @Singleton
    abstract fun bindDriveUploader(impl: SafDriveUploader): DriveUploader
}
