package com.eva.database.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Query
import androidx.room3.Upsert
import com.eva.database.entity.RecordingBookMarkEntity
import kotlinx.coroutines.flow.Flow
import org.jetbrains.annotations.VisibleForTesting

@Dao
interface RecordingsBookmarkDao {

	@Upsert
	suspend fun insertOrUpdateBookmark(bookmark: RecordingBookMarkEntity): Long

	@Upsert
	suspend fun insertOrUpdateBookmarks(bookmarks: Collection<RecordingBookMarkEntity>)

	@Query("SELECT * FROM RECORDING_BOOKMARK_TABLE WHERE RECORDING_ID=:recordId")
	fun getBookMarksFromRecordingIdAsFlow(recordId: Long): Flow<List<RecordingBookMarkEntity>>

	@Query("SELECT * FROM RECORDING_BOOKMARK_TABLE WHERE RECORDING_ID=:recordId")
	suspend fun getBookMarksFromRecordingId(recordId: Long): List<RecordingBookMarkEntity>

	@Query("SELECT * FROM RECORDING_BOOKMARK_TABLE WHERE BOOKMARK_ID=:bookmarkId")
	suspend fun getBookMarkFromBookMarkId(bookmarkId: Long): RecordingBookMarkEntity?

	@Query("DELETE FROM RECORDING_BOOKMARK_TABLE WHERE RECORDING_ID=:recordId")
	suspend fun deleteBookMarksByRecordingId(recordId: Long)

	@Delete
	suspend fun deleteBookMark(bookmark: RecordingBookMarkEntity)

	@Delete
	suspend fun deleteBookMarks(bookmarks: Collection<RecordingBookMarkEntity>)

	@VisibleForTesting
	@Query("DELETE FROM RECORDING_BOOKMARK_TABLE")
	suspend fun clearAllBookmarkData()
}