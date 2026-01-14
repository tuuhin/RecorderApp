package com.eva.recorder.data.recorder

import android.util.Log
import com.eva.recorder.domain.recorder.AudioByteDataProvider
import com.eva.recorder.domain.recorder.TranscriptionProvider
import com.eva.transcribe.domain.AudioTranscriptor
import com.eva.transcribe.domain.LanguageModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive

private const val TAG = "TRANSCRIPTION_PROVIDER"

@OptIn(FlowPreview::class)
internal class TranscriptionProviderImpl(
	private val source: AudioByteDataProvider,
	private val transcriptor: AudioTranscriptor,
	private val isEnabled: Boolean = true,
) : TranscriptionProvider {

	override val transcription: Flow<String>
		get() = source.stream
			.buffer(capacity = 50, onBufferOverflow = BufferOverflow.SUSPEND)
			.filter { (buffer, size) -> buffer.isNotEmpty() && size > 0 }
			.mapNotNull { (pcmBuffer, readSize) ->
				if (!currentCoroutineContext().isActive) return@mapNotNull null
				val rmsValue = pcmBuffer.rms(readSize)
				if (rmsValue <= AudioTranscriptor.MIN_RMS_TO_RECOGNIZE) return@mapNotNull null
				val bufferCopy = pcmBuffer.copyOf(readSize)
				// feed this to transcriber
				transcriptor.recognizeAudio(bufferCopy, readSize)
			}
			.map { it.fullResult.ifBlank { it.partial } }
			.catch { e ->
				// Log your error here so the app doesn't die
				Log.e(TAG, "SOME ERROR: ${e.message}")
			}

	override suspend fun initTranscriptions() {
		if (!isEnabled) return
		transcriptor.setUp(LanguageModel.EN_IN)
	}

	override fun transcriptCleanUp() = transcriptor.cleanUp()
}