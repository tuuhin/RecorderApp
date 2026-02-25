package com.eva.recorder.di

import android.content.Context
import com.eva.recorder.data.RecorderWidgetInteracterImpl
import com.eva.recorder.data.recorder.AudioRecordAmplitudeReader
import com.eva.recorder.domain.RecorderWidgetInteractor
import com.eva.recorder.domain.recorder.AudioDataReader
import com.eva.recorder.domain.stopwatch.RecorderStopWatch
import com.eva.utils.RecorderConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object RecorderSingletonModule {

	@Provides
	@Singleton
	fun providesAudioBytesReader(@ApplicationContext context: Context): AudioDataReader =
		AudioRecordAmplitudeReader(context)

	@Provides
	@Singleton
	fun providesStopWatch(): RecorderStopWatch =
		RecorderStopWatch(delayTime = RecorderConstants.AMPS_READ_DELAY_RATE)


	@Provides
	@Singleton
	fun providesWidgetInteracter(@ApplicationContext context: Context): RecorderWidgetInteractor =
		RecorderWidgetInteracterImpl(context)
}