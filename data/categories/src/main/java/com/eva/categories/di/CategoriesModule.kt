package com.eva.categories.di

import android.content.Context
import com.eva.categories.data.RecordingsCategoryProviderImpl
import com.eva.categories.domain.provider.RecordingCategoryProvider
import com.eva.database.dao.RecordingCategoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CategoriesModule {

	@Provides
	@Singleton
	fun providesCategoriesProvider(
		@ApplicationContext context: Context,
		categoryDao: RecordingCategoryDao,
	): RecordingCategoryProvider = RecordingsCategoryProviderImpl(context, categoryDao)
}