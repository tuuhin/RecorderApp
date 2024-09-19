package com.eva.recorderapp.voice_recorder.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.eva.recorderapp.voice_recorder.data.database.entity.RecordingBookMarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingsBookmarkDao {

	@Upsert
	suspend fun addBookMark(bookmark: RecordingBookMarkEntity): Long

	@Upsert
	suspend fun addBookMarks(bookmarks: Collection<RecordingBookMarkEntity>)

	@Query("SELECT * FROM RECORDING_BOOKMARK_TABLE WHERE RECORDING_ID=:recordId")
	fun getBookMarksFromRecordingIdAsFlow(recordId: Long): Flow<List<RecordingBookMarkEntity>>

	@Query("DELETE FROM RECORDING_BOOKMARK_TABLE WHERE RECORDING_ID=:recordId")
	suspend fun deleteBookMarksByRecordingId(recordId: Long)

	@Delete
	suspend fun deleteBookMark(bookmark: RecordingBookMarkEntity)

	@Delete
	suspend fun deleteBookMarks(bookmarks: Collection<RecordingBookMarkEntity>)

}