package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.eva.recorderapp.voice_recorder.data.recorder.VoiceRecorderImpl
import com.eva.recorderapp.voice_recorder.data.service.NotificationHelper
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderFileProvider
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderStopWatch
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
object RecorderServiceModule {

	@Provides
	@ServiceScoped
	fun providesStopWatch(): RecorderStopWatch = RecorderStopWatch()

	@Provides
	@ServiceScoped
	fun providesVoiceRecorder(
		@ApplicationContext context: Context,
		fileProvider: RecorderFileProvider,
		stopWatch: RecorderStopWatch
	): VoiceRecorder = VoiceRecorderImpl(
		context = context,
		fileProvider = fileProvider,
		stopWatch = stopWatch
	)

	@Provides
	@ServiceScoped
	fun providesExoPlayer(@ApplicationContext context: Context): Player {
		val attibutes = AudioAttributes.Builder()
			.setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
			.setUsage(C.USAGE_MEDIA)
			.build()

		return ExoPlayer.Builder(context)
			.setAudioAttributes(attibutes, true)
			.build()
	}

	@Provides
	@ServiceScoped
	fun providesNotificationHelper(
		@ApplicationContext context: Context
	): NotificationHelper = NotificationHelper(context = context)
}