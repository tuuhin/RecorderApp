package com.eva.transcribe.data

import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.eva.transcribe.BuildConfig
import com.eva.transcribe.domain.AudioTranscriptor
import com.eva.transcribe.domain.models.TranscriptionResult
import com.eva.transcribe.domain.repository.STTModelsRepository
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
	private val repository: STTModelsRepository,
	private val json: Json,
) : AudioTranscriptor {

	private var _model: Model? = null
	private var _recognizer: Recognizer? = null

	init {
		val level = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
		LibVosk.setLogLevel(level)
	}

	override suspend fun setUp(modelId: String, sampleRate: Float): Result<Unit> {
		return try {
			if (_model != null && _recognizer != null) {
				// if this is already present only reset the recognizer
				_recognizer?.reset()
				Log.i(TAG, "RECOGNIZER ALREADY PRESENT RESTING")
				return Result.failure(Exception("Recognizer is already set"))
			}
			val sttModelResult = repository.readModelByLanguage(modelId)
			if (sttModelResult.isFailure) {
				Log.d(TAG, "CANNOT READ THE GIVEN MODEL TYPE")
				return Result.failure(
					sttModelResult.exceptionOrNull() ?: Exception("Cannot find model")
				)
			}
			val sttModel = sttModelResult.getOrThrow()
			val modelFile = sttModel.localModelURI?.toUri()?.toFile() ?: run {
				Log.d(TAG, "MODEL IS NOT DOWNLOADED")
				return Result.failure(Exception("Model absent download it to use"))
			}
			Log.d(TAG, "PREPARING MODEL AND RECOGNIZER | PATH :${modelFile.absolutePath}")
			val model = Model(modelFile.absolutePath).also { _model = it }
			_recognizer = Recognizer(model, sampleRate)
			Log.i(TAG, "RECOGNIZER LOADED AND READY TO ROCK!")
			Result.success(Unit)
		} catch (e: IOException) {
			Log.d(TAG, "SOME ERROR", e)
			Result.failure(e)
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