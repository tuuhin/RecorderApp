package com.eva.recorderapp.voice_recorder.data.recordings.utils

import com.eva.recorderapp.voice_recorder.data.database.entity.RecordingBookMarkEntity
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioBookmarkModel

fun RecordingBookMarkEntity.toModel(): AudioBookmarkModel = AudioBookmarkModel(
	bookMarkId = bookMarkId ?: 0L,
	text = text,
	recordingId = recordingId,
	timeStamp = timeStamp
)

fun AudioBookmarkModel.toEntity(): RecordingBookMarkEntity = RecordingBookMarkEntity(
	bookMarkId = bookMarkId,
	text = text,
	recordingId = recordingId,
	timeStamp = timeStamp
)