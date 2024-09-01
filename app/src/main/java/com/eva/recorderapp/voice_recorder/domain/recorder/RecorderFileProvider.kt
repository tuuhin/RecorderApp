package com.eva.recorderapp.voice_recorder.domain.recorder

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.recorder.models.RecordEncoderAndFormat
import java.io.File


interface RecorderFileProvider {

	suspend fun createFileForRecording(extension: String?): File

	suspend fun transferFileDataToStorage(
		file: File,
		format: RecordEncoderAndFormat
	): Resource<String?, Exception>

	suspend fun deleteCreatedFile(file: File): Boolean

}