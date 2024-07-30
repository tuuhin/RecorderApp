package com.eva.recorderapp.voice_recorder.domain.recorder

import android.net.Uri
import com.eva.recorderapp.common.Resource


interface RecorderFileProvider {

	suspend fun createUriForRecording(format: RecordEncoderAndFormat): Uri?

	suspend fun updateUriAfterRecording(file: Uri): Resource<Unit, Exception>

	suspend fun deleteUriIfNotPending(uri: Uri)


}