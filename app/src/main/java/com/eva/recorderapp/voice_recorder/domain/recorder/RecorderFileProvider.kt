package com.eva.recorderapp.voice_recorder.domain.recorder

import com.eva.recorderapp.voice_recorder.domain.recorder.models.RecordEncoderAndFormat
import java.io.File


interface RecorderFileProvider {

	suspend fun createFileForRecoring(extension: String?): File

	suspend fun transferFileDataToStorage(file: File, format: RecordEncoderAndFormat): Boolean

	suspend fun deleteCreatedFile(file: File): Boolean

}