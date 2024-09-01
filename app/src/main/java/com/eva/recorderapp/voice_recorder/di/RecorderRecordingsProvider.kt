package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import android.os.Build
import com.eva.recorderapp.voice_recorder.data.recordings.database.dao.TrashFileDao
import com.eva.recorderapp.voice_recorder.data.recordings.provider.TrashRecordingsProviderApi29Impl
import com.eva.recorderapp.voice_recorder.data.recordings.provider.TrashRecordingsProviderImpl
import com.eva.recorderapp.voice_recorder.data.recordings.provider.VoiceRecordingsProviderImpl
import com.eva.recorderapp.voice_recorder.data.util.RecordingsActionHelperImpl
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.TrashRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.util.RecordingsActionHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecorderRecordingsProvider {

	@Provides
	@Singleton
	fun providesRecordingsProvider(
		@ApplicationContext context: Context
	): VoiceRecordingsProvider = VoiceRecordingsProviderImpl(context)


	@Provides
	@Singleton
	fun providesTrashedRecordingsProvider(
		@ApplicationContext context: Context,
		trashMetaData: TrashFileDao
	): TrashRecordingsProvider =
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			TrashRecordingsProviderImpl(context = context)
		else
			TrashRecordingsProviderApi29Impl(context = context, trashMediaDao = trashMetaData)


	@Provides
	@Singleton
	fun providesShareRecordingsActionHelper(
		@ApplicationContext context: Context
	): RecordingsActionHelper = RecordingsActionHelperImpl(context)
}