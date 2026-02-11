package com.eva.worker.di

import android.content.Context
import com.eva.worker.controller.STTModelDownloadController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object WorkerControllerModule {

	@Provides
	@ViewModelScoped
	fun providesSTTController(@ApplicationContext context: Context): STTModelDownloadController =
		STTModelDownloadController(context)

}