package com.eva.recorderapp.voice_recorder.domain.files

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.models.AudioFileModel
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel

interface RecordingsActionHelper {

	fun shareAudioFiles(collection: List<RecordedVoiceModel>): Resource<Unit, Exception>

	fun shareAudioFile(audioFileModel: AudioFileModel): Resource<Unit, Exception>
}