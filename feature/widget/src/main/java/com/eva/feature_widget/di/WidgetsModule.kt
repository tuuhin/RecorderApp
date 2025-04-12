package com.eva.feature_widget.di

import android.content.Context
import com.eva.feature_widget.recorder.repository.RecorderWidgetRepo
import com.eva.feature_widget.recorder.repository.RecorderWidgetRepoImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WidgetsModule {

	@Provides
	@Singleton
	fun providesWidgetUpdater(@ApplicationContext context: Context)
			: RecorderWidgetRepo = RecorderWidgetRepoImpl(context)
}