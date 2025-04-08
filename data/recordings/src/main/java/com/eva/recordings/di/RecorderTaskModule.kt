package com.eva.recordings.di

import android.content.Context
import com.eva.recordings.data.task.RecordingsPathCorrectionTaskImpl
import com.eva.recordings.data.task.RemoveTrashRecordingTaskImpl
import com.eva.recordings.domain.provider.TrashRecordingsProvider
import com.eva.recordings.domain.provider.VoiceRecordingsProvider
import com.eva.recordings.domain.tasks.RecordingPathCorrectionTask
import com.eva.recordings.domain.tasks.RemoveTrashRecordingTask
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecorderTaskModule {

	@Provides
	@Singleton
	fun providesUriPathCorrectorTask(
		@ApplicationContext context: Context,
		provider: VoiceRecordingsProvider,
	): RecordingPathCorrectionTask = RecordingsPathCorrectionTaskImpl(context, provider)

	@Provides
	@Singleton
	fun providesRemoveTrashTask(provider: TrashRecordingsProvider): RemoveTrashRecordingTask =
		RemoveTrashRecordingTaskImpl(provider)
}