package com.eva.speech_enhancements.di

import android.content.Context
import com.eva.speech_enhancements.ClearSpeechEnhancer
import com.eva.speech_enhancements.IClearSpeechEnhancer
import com.eva.speech_enhancements.model_manager.SpeechModelDownloader
import com.eva.speech_enhancements.model_manager.SpeechModelReader
import com.eva.speech_enhancements.model_manager.data.SpeechModelManifestReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
internal object SpeechEnhancerModule {

    @Provides
    @Singleton
    fun providesSpeechModelManifestReader(
        @ApplicationContext context: Context,
    ): SpeechModelManifestReader =
        SpeechModelManifestReader(context, Json { ignoreUnknownKeys = true })

    @Provides
    @Singleton
    fun providesSpeechModelReader(
        @ApplicationContext context: Context,
        manifestReader: SpeechModelManifestReader,
    ): SpeechModelReader = SpeechModelReader(context, manifestReader)

    @Provides
    @Singleton
    fun providesSpeechModelDownloader(
        reader: SpeechModelReader,
        httpClient: HttpClient,
    ): SpeechModelDownloader = SpeechModelDownloader(reader, httpClient)

    @Provides
    @Singleton
    fun providesClearSpeechEnhancer(
        modelReader: SpeechModelReader,
    ): IClearSpeechEnhancer = ClearSpeechEnhancer(modelReader)

}
