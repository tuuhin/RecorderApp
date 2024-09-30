package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import com.eva.recorderapp.voice_recorder.data.bookmarks.BookMarksToCsvFileConvertor
import com.eva.recorderapp.voice_recorder.data.bookmarks.RecordingBookMarkProviderImpl
import com.eva.recorderapp.voice_recorder.data.database.dao.RecordingsBookmarkDao
import com.eva.recorderapp.voice_recorder.domain.bookmarks.ExportBookMarkUriProvider
import com.eva.recorderapp.voice_recorder.domain.bookmarks.RecordingBookmarksProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BookmarksModule {

	@Provides
	@Singleton
	fun providesBookMarksProvider(
		@ApplicationContext context: Context,
	): ExportBookMarkUriProvider = BookMarksToCsvFileConvertor(context)

	@Provides
	@Singleton
	fun providesBookmarkProvider(
		bookmarkDao: RecordingsBookmarkDao,
	): RecordingBookmarksProvider = RecordingBookMarkProviderImpl(bookmarkDao)
}