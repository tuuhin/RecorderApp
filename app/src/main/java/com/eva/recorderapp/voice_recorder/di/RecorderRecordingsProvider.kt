package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import android.os.Build
import com.eva.recorderapp.voice_recorder.data.database.TrashFilesMetaDataDao
import com.eva.recorderapp.voice_recorder.data.files.RecordingsActionHelperImpl
import com.eva.recorderapp.voice_recorder.data.files.TrashRecordingsProviderApi29Impl
import com.eva.recorderapp.voice_recorder.data.files.TrashRecordingsProviderImpl
import com.eva.recorderapp.voice_recorder.data.files.VoiceRecordingsProviderImpl
import com.eva.recorderapp.voice_recorder.domain.files.RecordingsActionHelper
import com.eva.recorderapp.voice_recorder.domain.files.TrashRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.files.VoiceRecordingsProvider
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
		trashMetaData: TrashFilesMetaDataDao
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