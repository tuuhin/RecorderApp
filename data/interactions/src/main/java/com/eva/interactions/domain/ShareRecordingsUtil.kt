package com.eva.interactions.domain

import com.eva.bookmarks.domain.AudioBookmarkModel
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.utils.Resource

interface ShareRecordingsUtil {

	fun shareAudioFiles(collection: List<RecordedVoiceModel>): Resource<Unit, Exception>

	fun shareAudioFile(audioFileModel: AudioFileModel): Resource<Unit, Exception>

	suspend fun shareBookmarksCsv(bookmarks: Collection<AudioBookmarkModel>): Resource<Unit, Exception>
}