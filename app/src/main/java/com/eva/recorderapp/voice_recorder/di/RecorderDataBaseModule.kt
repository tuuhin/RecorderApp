package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import com.eva.recorderapp.voice_recorder.data.database.RecorderDataBase
import com.eva.recorderapp.voice_recorder.data.database.dao.RecordingCategoryDao
import com.eva.recorderapp.voice_recorder.data.database.dao.RecordingsBookmarkDao
import com.eva.recorderapp.voice_recorder.data.database.dao.RecordingsMetadataDao
import com.eva.recorderapp.voice_recorder.data.database.dao.TrashFileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecorderDataBaseModule {

	@Provides
	@Singleton
	fun providesRoomDatabase(
		@ApplicationContext context: Context,
	): RecorderDataBase = RecorderDataBase.createDataBase(context)

	@Provides
	@Singleton
	fun providesTrashDataDao(
		dataBase: RecorderDataBase,
	): TrashFileDao = dataBase.trashMetadataEntityDao()

	@Provides
	@Singleton
	fun providesCategoryDao(
		dataBase: RecorderDataBase,
	): RecordingCategoryDao = dataBase.categoriesDao()

	@Provides
	@Singleton
	fun providesRecordingsMetadataDao(
		dataBase: RecorderDataBase,
	): RecordingsMetadataDao = dataBase.recordingMetaData()

	@Provides
	@Singleton
	fun providesBookMarkDao(
		dataBase: RecorderDataBase,
	): RecordingsBookmarkDao = dataBase.recordingBookMarkDao()
}