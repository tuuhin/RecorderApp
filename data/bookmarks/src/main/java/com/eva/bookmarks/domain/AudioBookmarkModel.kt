package com.eva.bookmarks.domain

import kotlinx.datetime.LocalTime

data class AudioBookmarkModel(
	val bookMarkId: Long,
	val text: String,
	val recordingId: Long,
	val timeStamp: LocalTime,
)