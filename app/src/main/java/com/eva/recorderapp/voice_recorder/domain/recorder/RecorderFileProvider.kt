package com.eva.recorderapp.voice_recorder.domain.recorder

import java.io.File


interface RecorderFileProvider {

	suspend fun createFileForRecoring(): File

	suspend fun transferFileDataToStorage(file: File, format: RecordEncoderAndFormat): Boolean

	suspend fun deleteCreatedFile(file: File): Boolean

}