package com.eva.recordings.di

import android.content.Context
import android.os.Build
import com.eva.database.dao.RecordingsMetadataDao
import com.eva.database.dao.TrashFileDao
import com.eva.datastore.domain.repository.RecorderFileSettingsRepo
import com.eva.location.domain.repository.LocationAddressProvider
import com.eva.recordings.data.provider.PlayerFileProviderImpl
import com.eva.recordings.data.provider.RecorderFileProviderImpl
import com.eva.recordings.data.provider.RecordingSecondaryDataProviderImpl
import com.eva.recordings.data.provider.TrashRecordingsProviderApi29Impl
import com.eva.recordings.data.provider.TrashRecordingsProviderImpl
import com.eva.recordings.data.provider.VoiceRecordingsProviderImpl
import com.eva.recordings.domain.provider.PlayerFileProvider
import com.eva.recordings.domain.provider.RecorderFileProvider
import com.eva.recordings.domain.provider.RecordingsSecondaryDataProvider
import com.eva.recordings.domain.provider.TrashRecordingsProvider
import com.eva.recordings.domain.provider.VoiceRecordingsProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecordingsProviderModule {

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
	): TrashRecordingsProvider = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
		TrashRecordingsProviderImpl(context = context, recordingsDao = metadataDao)
	else TrashRecordingsProviderApi29Impl(
		context = context,
		trashMediaDao = trashMetaData,
		recordingsDao = metadataDao
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
	fun providesPlayerFileProvider(
		@ApplicationContext context: Context,
		locationProvider: LocationAddressProvider,
	): PlayerFileProvider = PlayerFileProviderImpl(context, locationProvider)


	@Provides
	@Singleton
	fun providesRecorderFileProvider(
		@ApplicationContext context: Context,
		recordingsDao: RecordingsMetadataDao,
		settings: RecorderFileSettingsRepo,
	): RecorderFileProvider = RecorderFileProviderImpl(context, recordingsDao, settings)

}