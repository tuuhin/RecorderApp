package com.eva.recorderapp.domain.voice_recorder

import android.net.Uri
import com.eva.recorderapp.domain.models.RecordedVoiceModel

interface RecorderFileProvider {

	suspend fun getAppVoiceRecordings(): List<RecordedVoiceModel>

	suspend fun createFileForRecording(): Uri?

	suspend fun updateFileAfterRecording(file: Uri): Boolean

	suspend fun cancelFileCreation(file: Uri): Boolean
}