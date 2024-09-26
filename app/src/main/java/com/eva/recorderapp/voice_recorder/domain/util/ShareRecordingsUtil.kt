package com.eva.recorderapp.voice_recorder.domain.util

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioFileModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel

interface ShareRecordingsUtil {

	fun shareAudioFiles(collection: List<RecordedVoiceModel>): Resource<Unit, Exception>

	fun shareAudioFile(audioFileModel: AudioFileModel): Resource<Unit, Exception>
}