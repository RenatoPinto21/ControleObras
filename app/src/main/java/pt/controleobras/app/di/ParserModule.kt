package pt.controleobras.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pt.controleobras.app.core.parser.HeuristicReceiptParser
import pt.controleobras.app.core.parser.ReceiptParser
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ParserModule {

    @Binds
    @Singleton
    abstract fun bindReceiptParser(impl: HeuristicReceiptParser): ReceiptParser
}
