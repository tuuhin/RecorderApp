package com.eva.recorder_native

import android.util.Log
import com.eva.recorder_native.exception.InvalidHandleResultException
import com.eva.recorder_native.exception.RecorderAlreadyInitiatedException
import com.eva.recorder_native.exception.RecorderHandleException
import com.eva.recorder_native.models.RecorderConfig
import java.io.File
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndUpdate

@OptIn(ExperimentalAtomicApi::class)
class WavAudioRecorder : CustomRecorder {

	private val _nativeHandle = AtomicLong(0L)
	private val _isInitialized = AtomicBoolean(false)

	private val _lock = Any()

	override fun create() = synchronized(_lock) {
		if (_isInitialized.load()) throw RecorderAlreadyInitiatedException()

		val handle = createRecorder()
		if (handle == 0L) throw InvalidHandleResultException()

		if (!_isInitialized.compareAndSet(expectedValue = false, newValue = true)) {
			releaseNative(handle)
			throw IllegalStateException("Recorder was init on another thread")
		}
		Log.i(TAG, "RECORDER INSTANCE CREATED")
		_nativeHandle.store(handle)
	}

	override fun release() = synchronized(_lock) {
		if (!_isInitialized.compareAndSet(expectedValue = true, newValue = false)) {
			Log.w(TAG, "RECORDER WAS NOT CREATED")
			return@synchronized
		}
		val handle = _nativeHandle.fetchAndUpdate { 0L }
		if (handle != 0L) {
			releaseNative(handle)
			Log.i(TAG, "RECORDER INSTANCE RELEASED")
		}
	}

	override val isRecording: Boolean
		get() {
			if (!_isInitialized.load()) {
				Log.w(TAG, "RECORDER WAS NOT INITIATED")
				throw RecorderHandleException()
			}
			val handle = requireNativeHandle()
			return isRecordRunning(handle)
		}


	override fun setOutputFile(file: File) {
		if (!_isInitialized.load()) {
			Log.w(TAG, "RECORDER WAS NOT INITIATED")
			throw RecorderHandleException()
		}
		val handle = requireNativeHandle()
		setOutputFileConfig(handle, file.absolutePath)
	}

	override fun setRecorderConfig(config: RecorderConfig) {
		if (!_isInitialized.load()) {
			Log.w(TAG, "RECORDER WAS NOT INITIATED")
			throw RecorderHandleException()
		}
		val handle = requireNativeHandle()
		setConfig(handle, config.sampleRate, config.channel)
	}

	fun prepare() {
		if (!_isInitialized.load()) {
			Log.w(TAG, "RECORDER WAS NOT INITIATED")
			throw RecorderHandleException()
		}
		val handle = requireNativeHandle()
		prepareRecorder(handle)
	}

	fun start() {
		if (!_isInitialized.load()) {
			Log.w(TAG, "RECORDER WAS NOT INITIATED")
			throw RecorderHandleException()
		}
		val handle = requireNativeHandle()
		startRecorder(handle)
	}

	fun stop() {
		if (!_isInitialized.load()) {
			Log.w(TAG, "RECORDER WAS NOT INITIATED")
			throw RecorderHandleException()
		}
		val handle = requireNativeHandle()
		stopRecorder(handle)
	}

	fun pause() {
		if (!_isInitialized.load()) {
			Log.w(TAG, "RECORDER WAS NOT INITIATED")
			throw RecorderHandleException()
		}
		val handle = requireNativeHandle()
		pauseRecorder(handle)
	}

	fun resume() {
		if (!_isInitialized.load()) {
			Log.w(TAG, "RECORDER WAS NOT INITIATED")
			throw RecorderHandleException()
		}
		val handle = requireNativeHandle()
		resumeRecorder(handle)
	}

	fun reset() {
		if (!_isInitialized.load()) {
			Log.w(TAG, "RECORDER WAS NOT INITIATED")
			throw RecorderHandleException()
		}
		val handle = requireNativeHandle()
		resetRecorder(handle)
	}

	private fun requireNativeHandle(): Long = synchronized(_lock) {
		_nativeHandle.load().takeIf { it != 0L }
			?: throw RecorderHandleException()
	}

	// create and release
	private external fun createRecorder(): Long
	private external fun releaseNative(handle: Long)

	// audio and file configuration
	private external fun setConfig(handle: Long, sampleRate: Int, channels: Int)
	private external fun setOutputFileConfig(handle: Long, filePath: String)

	// lifecycle
	private external fun prepareRecorder(handle: Long)
	private external fun startRecorder(handle: Long)
	private external fun stopRecorder(handle: Long)
	private external fun pauseRecorder(handle: Long)
	private external fun resumeRecorder(handle: Long)
	private external fun resetRecorder(handle: Long)

	// helpers
	private external fun isRecordRunning(handle: Long): Boolean

	companion object {

		private const val TAG = "WavAudioRecorder"

		init {
			System.loadLibrary("wav_recorder")
		}
	}
}
