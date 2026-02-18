package com.eva.transcribe.di

import com.eva.transcribe.data.AudioTranscriptorImpl
import com.eva.transcribe.domain.AudioTranscriptor
import com.eva.transcribe.domain.repository.STTModelsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TranscriberModule {

	@Provides
	@Singleton
	fun providersTranscriptor(
		repository: STTModelsRepository,
		json: Json
	): AudioTranscriptor = AudioTranscriptorImpl(repository, json = json)
}