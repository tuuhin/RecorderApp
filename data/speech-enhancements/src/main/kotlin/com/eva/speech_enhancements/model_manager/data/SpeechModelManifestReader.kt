package com.eva.speech_enhancements.model_manager.data

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

internal class SpeechModelManifestReader(
    private val context: Context,
    private val json: Json,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun read(): SpeechModelsAssets = withContext(dispatcher) {
        context.assets.open(MANIFEST_FILE)
            .use { json.decodeFromStream<SpeechModelsAssets>(it) }
    }

    companion object {
        private const val MANIFEST_FILE = "models.json"
    }
}
