package com.eva.transcribe.data.repository

import android.content.Context
import android.util.Log
import com.eva.transcribe.data.models.ModelsMetadataList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

private const val TAG = "ASSETS_READER"

internal class STTModelMetadataReader(
	private val json: Json,
	private val context: Context,
) {

	@OptIn(ExperimentalSerializationApi::class)
	suspend fun readMetaData(): Result<ModelsMetadataList> {
		return withContext(Dispatchers.IO) {
			try {
				val result = context.assets.open(METADATA_FILE)
					.use { stream -> json.decodeFromStream<ModelsMetadataList>(stream) }
				Result.success(result)
			} catch (e: Exception) {
				if (e is CancellationException) throw e
				Log.e(TAG, "METADATA FILE CANNOT BE READ", e)
				Result.failure(e)
			}
		}
	}

	companion object {
		private const val METADATA_FILE = "vosk_models_metadata.json"
	}
}