package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import com.eva.recorderapp.voice_recorder.data.recordings.database.RecorderDataBase
import com.eva.recorderapp.voice_recorder.data.recordings.database.TrashFileDao
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
		@ApplicationContext context: Context
	): RecorderDataBase = RecorderDataBase.createDataBase(context)

	@Provides
	@Singleton
	fun providesTrashDataDao(
		dataBase: RecorderDataBase
	): TrashFileDao = dataBase.trashMetadataEntityDao()
}