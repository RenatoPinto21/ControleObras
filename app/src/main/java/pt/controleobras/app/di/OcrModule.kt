package pt.controleobras.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pt.controleobras.app.core.ocr.MlKitTextRecognizer
import pt.controleobras.app.core.ocr.TextRecognizer
import pt.controleobras.app.core.qr.MlKitQrCodeReader
import pt.controleobras.app.core.qr.QrCodeReader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OcrModule {

    @Binds
    @Singleton
    abstract fun bindTextRecognizer(impl: MlKitTextRecognizer): TextRecognizer

    @Binds
    @Singleton
    abstract fun bindQrCodeReader(impl: MlKitQrCodeReader): QrCodeReader
}
