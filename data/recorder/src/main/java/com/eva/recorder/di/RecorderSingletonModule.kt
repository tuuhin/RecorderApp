package com.eva.recorder.di

import android.content.Context
import com.eva.recorder.data.RecorderWidgetInteracterImpl
import com.eva.recorder.domain.RecorderWidgetInteractor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecorderSingletonModule {

	@Provides
	@Singleton
	fun providesWidgetInteracter(@ApplicationContext context: Context): RecorderWidgetInteractor =
		RecorderWidgetInteracterImpl(context)
}