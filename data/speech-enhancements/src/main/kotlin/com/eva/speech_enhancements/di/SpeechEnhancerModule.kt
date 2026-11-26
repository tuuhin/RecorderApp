package com.eva.speech_enhancements.di

import android.content.Context
import com.eva.speech_enhancements.ClearSpeechEnhancer
import com.eva.speech_enhancements.IClearSpeechEnhancer
import com.eva.speech_enhancements.model_manager.SpeechModelManagerImpl
import com.eva.speech_enhancements.model_manager.SpeechModelProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SpeechEnhancerModule {

    @Provides
    @Singleton
    fun providesSpeechModelManager(@ApplicationContext context: Context): SpeechModelProvider =
        SpeechModelManagerImpl(context)

    @Provides
    @Singleton
    fun providesClearSpeechEnhancer(
        speechModelManager: SpeechModelProvider,
    ): IClearSpeechEnhancer = ClearSpeechEnhancer(speechModelManager)
}
