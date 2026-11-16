package com.eva.player.di

import android.content.Context
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.player.R
import com.eva.player.data.service.AudioPlayerMediaCallBacks
import com.eva.player.data.service.AudioPlayerNotification
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Named
import kotlinx.coroutines.runBlocking

@Module
@UnstableApi
@InstallIn(ServiceComponent::class)
object PlayerServiceModule {

    @Provides
    @ServiceScoped
    @Named("SERVICE_PLAYER")
    fun providesExoPlayer(
        @ApplicationContext context: Context,
        settings: RecorderAudioSettingsRepo,
        mediaSource: MediaSource.Factory,
        attributes: AudioAttributes,
    ): Player {

        val audioSettings = runBlocking { settings.audioSettings() }

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSource)
            .setSkipSilenceEnabled(audioSettings.skipSilences)
            .setAudioAttributes(attributes, true)
            .setTrackSelector(DefaultTrackSelector(context))
            .setName("SERVICE_PLAYER")
            .build()
    }

    @Provides
    @ServiceScoped
    @UnstableApi
    fun providesPlayerNotification(
        @ApplicationContext context: Context,
    ): MediaNotification.Provider {
        return AudioPlayerNotification(context).apply {
            setSmallIcon(R.drawable.ic_record_player)
        }
    }

    @Provides
    @ServiceScoped
    fun providesServiceSessions(
        @ApplicationContext context: Context,
        @Named("SERVICE_PLAYER") player: Player,
    ): MediaSession {

        val callback = AudioPlayerMediaCallBacks(context)

        val extras = Bundle().apply {
            putBoolean(MediaConstants.EXTRAS_KEY_SLOT_RESERVATION_SEEK_TO_NEXT, true)
            putBoolean(MediaConstants.EXTRAS_KEY_SLOT_RESERVATION_SEEK_TO_PREV, true)
        }

        return MediaSession.Builder(context, player)
            .setExtras(extras)
            .setCallback(callback)
            .build()
    }
}
