package com.eva.recorderapp.voice_recorder.domain.bookmarks

import kotlinx.datetime.LocalTime

data class AudioBookmarkModel(
	val bookMarkId: Long,
	val text: String,
	val recordingId: Long,
	val timeStamp: LocalTime,
)
