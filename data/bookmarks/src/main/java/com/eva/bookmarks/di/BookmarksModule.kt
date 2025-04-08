package com.eva.bookmarks.di

import android.content.Context
import com.eva.bookmarks.data.BookMarksToCsvFileConvertor
import com.eva.bookmarks.data.RecordingBookMarkProviderImpl
import com.eva.bookmarks.domain.provider.ExportBookMarkUriProvider
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
	fun providesBookmarksToCsvConvertor(@ApplicationContext context: Context)
			: ExportBookMarkUriProvider = BookMarksToCsvFileConvertor(context)

	@Provides
	@Singleton
	fun providesBookMarksProvider(
		dao: RecordingsBookmarkDao,
		provider: RecordingsSecondaryDataProvider,
	): RecordingBookmarksProvider = RecordingBookMarkProviderImpl(dao, provider)
}