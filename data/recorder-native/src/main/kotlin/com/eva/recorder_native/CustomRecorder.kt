package com.eva.recorder_native

import com.eva.recorder_native.models.RecorderConfig
import java.io.File

interface CustomRecorder {

	fun create()

	fun release()

	val isRecording: Boolean

	fun setOutputFile(file: File)

	fun setRecorderConfig(config: RecorderConfig = RecorderConfig())
}