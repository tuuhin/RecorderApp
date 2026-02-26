package com.eva.recorder.data.recorder

import android.util.Log
import com.eva.datastore.domain.repository.TranscriptionSettingsRepo
import com.eva.recorder.domain.recorder.AudioByteDataProvider
import com.eva.recorder.domain.recorder.TranscriptionProvider
import com.eva.transcribe.domain.AudioTranscriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

private const val TAG = "TRANSCRIPTION_PROVIDER"

internal class TranscriptionProviderImpl(
	private val source: AudioByteDataProvider,
	private val transcriptor: AudioTranscriptor,
	private val settingsRepo: TranscriptionSettingsRepo,
) : TranscriptionProvider {

	@OptIn(ExperimentalCoroutinesApi::class)
	override val transcription: Flow<String>
		get() = settingsRepo.settingsFlow.flatMapLatest { settings ->
			if (settings.isEnabled && settings.modelId != null) readTranscriptionFlow()
			else emptyFlow()
		}

	private fun readTranscriptionFlow() = source.stream
		.buffer(capacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
		.filter { (_, size) -> size > 0 }
		.mapNotNull { (pcmBuffer, readSize) ->
			val rmsValue = pcmBuffer.rms(readSize)
			if (rmsValue <= AudioTranscriptor.MIN_RMS_TO_RECOGNIZE) return@mapNotNull null
			try {
				// feed this to transcriber
				transcriptor.recognizeAudio(pcmBuffer, readSize)
			} catch (e: Exception) {
				Log.e(TAG, "ERROR IN TRANSCRIPTION", e)
				return@mapNotNull null
			}
		}
		.flowOn(Dispatchers.Default)
		.map { result ->
			// using partial results are good for live transcriptions
			// a full result is mostly contamination of all the partial results
			result.partial
		}
		.catch { e ->
			// Log your error here so the app doesn't die
			Log.e(TAG, "SOME ERROR: ${e.message}")
		}

	override suspend fun initTranscriptions() {
		val settings = settingsRepo.setting()
		val currentModel = settings.modelId
		if (!settings.isEnabled || currentModel == null) {
			Log.d(TAG, "TRANSLATIONS NOT ENABLED OR NO MODEL IS SELECTED")
			return
		}
		transcriptor.setUp(currentModel)
	}

	override fun transcriptCleanUp() = transcriptor.cleanUp()
}