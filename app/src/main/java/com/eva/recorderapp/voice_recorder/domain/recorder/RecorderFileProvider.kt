package com.eva.recorderapp.voice_recorder.domain.recorder

import android.net.Uri
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel

interface RecorderFileProvider {

	suspend fun getAppVoiceRecordings(): List<RecordedVoiceModel>

	suspend fun createFileForRecording(): Uri?

	suspend fun updateFileAfterRecording(file: Uri): Boolean

	suspend fun cancelFileCreation(file: Uri): Boolean
}