@file:androidx.media3.common.util.UnstableApi

package com.eva.player.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.amr.AmrExtractor
import androidx.media3.extractor.flac.FlacExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import com.eva.player.data.AudioMetadataRetrieverImpl
import com.eva.player.data.processor.SpeechEnhancementProcessor
import com.eva.player.domain.AudioMetadataRetriever
import com.eva.speech_enhancements.IClearSpeechEnhancer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

typealias MediaSourceFactory = MediaSource.Factory

@Module
@InstallIn(SingletonComponent::class)
object PlayerCommonModule {

    @Provides
    @Singleton
    fun providesPlayerAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .setSpatializationBehavior(C.SPATIALIZATION_BEHAVIOR_AUTO)
            .build()
    }

    @Provides
    @Singleton
    fun providesMediaExtractorFactory(@ApplicationContext context: Context): MediaSource.Factory {

        val extractor = DefaultExtractorsFactory().apply {
            //set extractor flags later if there is some problem
            setAmrExtractorFlags(AmrExtractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)
            setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)
            setFlacExtractorFlags(FlacExtractor.FLAG_DISABLE_ID3_METADATA)
        }

        return DefaultMediaSourceFactory(context, extractor)
    }

    @Provides
    @Singleton
    fun providesAudioMetadataRetriever(
        @ApplicationContext context: Context,
        mediaSource: MediaSourceFactory,
    ): AudioMetadataRetriever = AudioMetadataRetrieverImpl(context, mediaSource)

    @Provides
    @IntoSet
    fun providesSpeechEnhancementProcessor(
        speechEnhancer: IClearSpeechEnhancer,
    ): AudioProcessor = SpeechEnhancementProcessor(speechEnhancer)


    @Provides
    @Singleton
    fun providesCustomRendersFactory(
        @ApplicationContext context: Context,
        processors: Set<@JvmSuppressWildcards AudioProcessor> = emptySet(),
    ): RenderersFactory = object : DefaultRenderersFactory(context) {

        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioOutputPlaybackParams: Boolean,
        ): AudioSink {
            val audioProcessors = processors.toTypedArray()

            return DefaultAudioSink.Builder(context)
                .setAudioProcessors(audioProcessors)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
                .build()
        }
    }
}
