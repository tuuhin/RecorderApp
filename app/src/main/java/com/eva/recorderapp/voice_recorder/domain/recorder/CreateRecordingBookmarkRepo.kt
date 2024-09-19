package com.eva.recorderapp.voice_recorder.domain.recorder

import com.eva.recorderapp.common.Resource
import kotlinx.datetime.LocalTime

interface CreateRecordingBookmarkRepo {

	suspend fun createBookMarks(
		recordingId: Long,
		points: Collection<LocalTime>,
	): Resource<Unit, Exception>

}