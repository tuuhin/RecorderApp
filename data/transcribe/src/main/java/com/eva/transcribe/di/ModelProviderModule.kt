package com.eva.transcribe.di

import android.content.Context
import com.eva.transcribe.data.ModelDownloadMangerImpl
import com.eva.transcribe.data.ModelFileProviderImpl
import com.eva.transcribe.domain.ModelDownloadManager
import com.eva.transcribe.domain.ModelFileProvider
import com.eva.transcribe.domain.models.TranscriptionResult
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ModelProviderModule {

	@Provides
	@Singleton
	fun providesJsonConfig(): Json = Json {
		serializersModule = SerializersModule {
			serializer<TranscriptionResult>()
		}
	}

	@Provides
	@Singleton
	fun providesHTTPClient(): HttpClient = HttpClient(Android) {
		install(Logging) {
			level = LogLevel.INFO
		}
	}

	@Provides
	@Singleton
	fun providesModelDownloader(
		@ApplicationContext context: Context,
		client: HttpClient
	): ModelDownloadManager = ModelDownloadMangerImpl(context, client)


	@Provides
	@Singleton
	fun providesModelFileProvider(@ApplicationContext context: Context): ModelFileProvider =
		ModelFileProviderImpl(context)
}