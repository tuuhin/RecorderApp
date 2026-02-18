package com.eva.transcribe.di

import android.content.Context
import com.eva.database.dao.STTModelsDao
import com.eva.transcribe.BuildConfig
import com.eva.transcribe.data.ModelDownloadMangerImpl
import com.eva.transcribe.data.repository.STTModelMetadataReader
import com.eva.transcribe.data.repository.STTModelsRepositoryImpl
import com.eva.transcribe.domain.ModelDownloadManager
import com.eva.transcribe.domain.models.TranscriptionResult
import com.eva.transcribe.domain.repository.STTModelsRepository
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
internal object ModelProviderModule {

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
			level = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
		}
	}

	@Provides
	@Singleton
	fun providesModelDownloader(
		@ApplicationContext context: Context,
		client: HttpClient,
		repo: STTModelsRepository,
	): ModelDownloadManager = ModelDownloadMangerImpl(
		context = context,
		httpClient = client,
		repository = repo
	)


	@Provides
	@Singleton
	fun providesAssetsReader(
		@ApplicationContext context: Context,
		json: Json
	): STTModelMetadataReader = STTModelMetadataReader(json, context)

	@Provides
	@Singleton
	fun providesSTTModelsRepo(
		dao: STTModelsDao,
		reader: STTModelMetadataReader
	): STTModelsRepository = STTModelsRepositoryImpl(dao = dao, metaData = reader)
}