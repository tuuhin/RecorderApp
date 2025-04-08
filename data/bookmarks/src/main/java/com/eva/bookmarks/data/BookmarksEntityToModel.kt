package com.eva.bookmarks.data

import com.eva.bookmarks.domain.AudioBookmarkModel
import com.eva.database.entity.RecordingBookMarkEntity

internal fun RecordingBookMarkEntity.toModel(): AudioBookmarkModel = AudioBookmarkModel(
	bookMarkId = bookMarkId ?: 0L,
	text = text,
	recordingId = recordingId,
	timeStamp = timeStamp
)

internal fun AudioBookmarkModel.toEntity(): RecordingBookMarkEntity = RecordingBookMarkEntity(
	bookMarkId = bookMarkId,
	text = text,
	recordingId = recordingId,
	timeStamp = timeStamp
)