package com.eva.transcribe.di

import android.content.Context
import com.eva.transcribe.data.AudioTranscriptorImpl
import com.eva.transcribe.data.ModelFileProviderImpl
import com.eva.transcribe.domain.AudioTranscriptor
import com.eva.transcribe.domain.ModelFileProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TranscriberModule {

	@Provides
	@Singleton
	fun providesModelFileProvider(@ApplicationContext context: Context): ModelFileProvider =
		ModelFileProviderImpl(context)

	@Provides
	@Singleton
	fun providersTranscriptor(provider: ModelFileProvider): AudioTranscriptor =
		AudioTranscriptorImpl(provider)
}