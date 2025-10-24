package com.eva.bookmarks.data.di

import android.content.Context
import com.eva.bookmarks.data.BookMarksToCsvFileConvertor
import com.eva.bookmarks.data.RecordingBookMarkProviderImpl
import com.eva.bookmarks.data.data.TestExportURIProvider
import com.eva.bookmarks.di.BookmarksModule
import com.eva.bookmarks.domain.provider.BookMarksExportRepository
import com.eva.bookmarks.domain.provider.ExportURIProvider
import com.eva.bookmarks.domain.provider.RecordingBookmarksProvider
import com.eva.database.dao.RecordingsBookmarkDao
import com.eva.recordings.domain.provider.RecordingsSecondaryDataProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton


@Module
@TestInstallIn(
	components = [SingletonComponent::class],
	replaces = [BookmarksModule::class]
)
object BookmarksTestModule {

	@Provides
	@Singleton
	fun providesTestExportURIProvider(@ApplicationContext context: Context): ExportURIProvider =
		TestExportURIProvider(context)

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