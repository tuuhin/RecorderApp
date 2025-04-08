package com.eva.database.di

import android.content.Context
import com.eva.database.RecorderDataBase
import com.eva.database.dao.RecordingCategoryDao
import com.eva.database.dao.RecordingsBookmarkDao
import com.eva.database.dao.RecordingsMetadataDao
import com.eva.database.dao.TrashFileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
	@Provides
	@Singleton
	fun providesRoomDatabase(
		@ApplicationContext context: Context,
	): RecorderDataBase = RecorderDataBase.createDataBase(context)

	@Provides
	@Singleton
	fun providesTrashDataDao(dataBase: RecorderDataBase)
			: TrashFileDao = dataBase.trashMetadataEntityDao()

	@Provides
	@Singleton
	fun providesCategoryDao(dataBase: RecorderDataBase)
			: RecordingCategoryDao = dataBase.categoriesDao()

	@Provides
	@Singleton
	fun providesRecordingsMetadataDao(dataBase: RecorderDataBase)
			: RecordingsMetadataDao = dataBase.recordingMetaData()

	@Provides
	@Singleton
	fun providesBookMarkDao(dataBase: RecorderDataBase)
			: RecordingsBookmarkDao = dataBase.recordingBookMarkDao()
}