package com.eva.transcribe.data

import android.util.Log
import com.eva.transcribe.BuildConfig
import com.eva.transcribe.domain.AudioTranscriptor
import com.eva.transcribe.domain.ModelFileProvider
import com.eva.transcribe.domain.models.LanguageModel
import com.eva.transcribe.domain.models.TranscriptionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "AUDIO_TRANSCRIPTOR"

internal class AudioTranscriptorImpl(
	private val pathProvider: ModelFileProvider,
	private val json: Json,
) : AudioTranscriptor {

	private var _model: Model? = null
	private var _recognizer: Recognizer? = null

	init {
		val level = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
		LibVosk.setLogLevel(level)
	}

	override suspend fun setUp(language: LanguageModel, sampleRate: Float) {
		try {
			if (_model != null && _recognizer != null) {
				// if this is already present only reset the recognizer
				_recognizer?.reset()
				Log.i(TAG, "RECOGNIZER ALREADY PRESENT RESTING")
				return
			}
			val file = pathProvider.provideModelFile(language) ?: return
			val model = Model(file.absolutePath).also { _model = it }
			_recognizer = Recognizer(model, sampleRate)
			Log.i(TAG, "RECOGNIZER LOADED AND READY TO ROCK!")
		} catch (e: IOException) {
			Log.d(TAG, "SOME ERROR", e)
		}
	}

	override suspend fun recognizeAudio(buffer: ShortArray, length: Int): TranscriptionResult? {
		val recognizer = _recognizer ?: run {
			Log.d(TAG, "UNABLE TO SET UP RECOGNIZED")
			return null
		}
		return withContext(Dispatchers.Default) {
			try {
				val success = recognizer.acceptWaveForm(buffer, length)
				val jsonString = if (success) recognizer.result else recognizer.partialResult
				json.decodeFromString<TranscriptionResult>(jsonString)
			} catch (_: CancellationException) {
				Log.d(TAG, "COROUTINE IS CANCELLED")
				return@withContext null
			}
		}
	}

	override fun cleanUp() {
		// close the recognizer
		_recognizer?.close()
		_recognizer = null
		Log.i(TAG, "CLOSING THE RECOGNIZER")
		// close the model
		_model?.close()
		_model = null
		Log.i(TAG, "CLOSING THE MODEL")
	}
}