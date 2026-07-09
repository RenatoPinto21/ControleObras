package pt.controleobras.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pt.controleobras.app.core.llm.LlmExtractor
import pt.controleobras.app.core.llm.MediaPipeLlmExtractor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LlmModule {

    @Binds
    @Singleton
    abstract fun bindLlmExtractor(impl: MediaPipeLlmExtractor): LlmExtractor
}
