package com.eva.recorder.di

import android.content.Context
import com.eva.recorder.data.RecorderActionHandlerImpl
import com.eva.recorder.domain.RecorderActionHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object RecorderActionHandlerModule {

	@Provides
	@ViewModelScoped
	fun providesActionHandler(@ApplicationContext context: Context)
			: RecorderActionHandler = RecorderActionHandlerImpl(context)
}