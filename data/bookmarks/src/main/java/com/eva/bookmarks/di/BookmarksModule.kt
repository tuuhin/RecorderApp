package com.eva.bookmarks.di

import android.content.Context
import com.eva.bookmarks.data.AndroidExportURIProvider
import com.eva.bookmarks.data.BookMarksToCsvFileConvertor
import com.eva.bookmarks.data.RecordingBookMarkProviderImpl
import com.eva.bookmarks.domain.provider.BookMarksExportRepository
import com.eva.bookmarks.domain.provider.ExportURIProvider
import com.eva.bookmarks.domain.provider.RecordingBookmarksProvider
import com.eva.database.dao.RecordingsBookmarkDao
import com.eva.recordings.domain.provider.RecordingsSecondaryDataProvider
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
	fun providesBookURIProvider(@ApplicationContext context: Context): ExportURIProvider =
		AndroidExportURIProvider(context)

	@Provides
	@Singleton
	fun providesBookmarksToCsvConvertor(
		provider: ExportURIProvider
	): BookMarksExportRepository = BookMarksToCsvFileConvertor(provider)

	@Provides
	@Singleton
	fun providesBookMarksProvider(
		dao: RecordingsBookmarkDao,
		provider: RecordingsSecondaryDataProvider,
	): RecordingBookmarksProvider = RecordingBookMarkProviderImpl(dao, provider)

}