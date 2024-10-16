package com.eva.recorderapp.voice_recorder.domain.bookmarks

import com.eva.recorderapp.common.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime

interface RecordingBookmarksProvider {

	fun getRecordingBookmarksFromId(audioId: Long): Flow<List<AudioBookmarkModel>>

	suspend fun getRecordingBookmarksFromIdAsList(audioId: Long): List<AudioBookmarkModel>

	suspend fun createBookMarks(recordingId: Long, points: Collection<LocalTime>)
			: Resource<Unit, Exception>

	suspend fun createBookMark(
		recordingId: Long,
		time: LocalTime,
		text: String,
	): Resource<AudioBookmarkModel, Exception>

	suspend fun deleteBookMarks(bookmarks: Collection<AudioBookmarkModel>): Resource<Unit, Exception>

	suspend fun updateBookMark(bookmark: AudioBookmarkModel, text: String?)
			: Resource<AudioBookmarkModel, Exception>
}