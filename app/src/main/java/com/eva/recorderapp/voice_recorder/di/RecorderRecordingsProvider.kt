package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import android.os.Build
import com.eva.recorderapp.voice_recorder.data.database.dao.RecordingCategoryDao
import com.eva.recorderapp.voice_recorder.data.database.dao.RecordingsMetadataDao
import com.eva.recorderapp.voice_recorder.data.database.dao.TrashFileDao
import com.eva.recorderapp.voice_recorder.data.recordings.provider.RecordingSecondaryDataProviderImpl
import com.eva.recorderapp.voice_recorder.data.recordings.provider.RecordingsCategoryProviderImpl
import com.eva.recorderapp.voice_recorder.data.recordings.provider.TrashRecordingsProviderApi29Impl
import com.eva.recorderapp.voice_recorder.data.recordings.provider.TrashRecordingsProviderImpl
import com.eva.recorderapp.voice_recorder.data.recordings.provider.VoiceRecordingsProviderImpl
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderFileSettingsRepo
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingCategoryProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingsSecondaryDataProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.TrashRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.use_cases.GetRecordingsOfCurrentAppUseCase
import com.eva.recorderapp.voice_recorder.domain.interactions.AppWidgetsRepository
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
		@ApplicationContext context: Context,
		fileSettingsRepo: RecorderFileSettingsRepo,
	): VoiceRecordingsProvider = VoiceRecordingsProviderImpl(context, fileSettingsRepo)

	@Provides
	@Singleton
	fun providesTrashedRecordingsProvider(
		@ApplicationContext context: Context,
		trashMetaData: TrashFileDao,
		metadataDao: RecordingsMetadataDao,
	): TrashRecordingsProvider =
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			TrashRecordingsProviderImpl(context = context, recordingsDao = metadataDao)
		else TrashRecordingsProviderApi29Impl(
			context = context,
			trashMediaDao = trashMetaData,
			recordingsDao = metadataDao
		)

	@Provides
	@Singleton
	fun providesRecordingsCategoryProvider(
		@ApplicationContext context: Context,
		categoryDao: RecordingCategoryDao,
	): RecordingCategoryProvider = RecordingsCategoryProviderImpl(
		context = context,
		categoryDao = categoryDao,
	)

	@Provides
	@Singleton
	fun providesSecondaryProvider(
		@ApplicationContext context: Context,
		recordingsMetadataDao: RecordingsMetadataDao,
	): RecordingsSecondaryDataProvider =
		RecordingSecondaryDataProviderImpl(context = context, recordingsDao = recordingsMetadataDao)

	@Provides
	@Singleton
	fun providesOwnerRecordingsUseCase(
		recordings: VoiceRecordingsProvider,
		secondaryRecordingsData: RecordingsSecondaryDataProvider,
		widgetFacade: AppWidgetsRepository,
	): GetRecordingsOfCurrentAppUseCase =
		GetRecordingsOfCurrentAppUseCase(recordings, secondaryRecordingsData, widgetFacade)

}