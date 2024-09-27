package com.eva.recorderapp.voice_recorder.data.recordings.provider

import android.database.sqlite.SQLiteException
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.data.database.dao.RecordingsBookmarkDao
import com.eva.recorderapp.voice_recorder.data.database.entity.RecordingBookMarkEntity
import com.eva.recorderapp.voice_recorder.data.recordings.utils.toEntity
import com.eva.recorderapp.voice_recorder.data.recordings.utils.toModel
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioBookmarkModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingBookmarksProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime

class RecordingBookMarkProviderImpl(
	private val bookmarkDao: RecordingsBookmarkDao,
) : RecordingBookmarksProvider {

	override fun getRecordingBookmarksFromId(audioId: Long): Flow<List<AudioBookmarkModel>> {
		return bookmarkDao.getBookMarksFromRecordingIdAsFlow(audioId).map { entities ->
			entities.map(RecordingBookMarkEntity::toModel)
		}.flowOn(Dispatchers.IO)
	}

	override suspend fun getRecordingBookmarksFromIdAsList(audioId: Long): List<AudioBookmarkModel> {
		return withContext(Dispatchers.IO) {
			bookmarkDao.getBookMarksFromRecordingId(audioId)
				.map(RecordingBookMarkEntity::toModel)
		}
	}

	override suspend fun createBookMarks(recordingId: Long, points: Collection<LocalTime>)
			: Resource<Unit, Exception> {
		val entities = points.map { point ->
			RecordingBookMarkEntity(recordingId = recordingId, timeStamp = point)
		}
		return try {
			withContext(Dispatchers.IO) {
				bookmarkDao.insertOrUpdateBookmarks(entities)
			}
			Resource.Success(Unit)
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, e.message ?: "")
		}
	}

	override suspend fun updateBookMark(
		updatedBookmark: AudioBookmarkModel,
		text: String?,
	): Resource<AudioBookmarkModel, Exception> {
		return try {
			val entity = updatedBookmark.toEntity().copy(text = text ?: "")

			val result = withContext(Dispatchers.IO) {
				bookmarkDao.insertOrUpdateBookmark(entity)
				bookmarkDao.getBookMarkFromBookMarkId(updatedBookmark.bookMarkId)
			}
			result?.let {
				Resource.Success(data = it.toModel())
			} ?: Resource.Error(Exception("Invalid bookmark id"))

		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, e.message ?: "")
		}
	}

	override suspend fun createBookMark(
		recordingId: Long,
		time: LocalTime,
		text: String,
	): Resource<AudioBookmarkModel, Exception> {
		return try {
			val result = withContext(Dispatchers.IO) {
				val entity = RecordingBookMarkEntity(
					text = text,
					recordingId = recordingId,
					timeStamp = time
				)
				val id = async { bookmarkDao.insertOrUpdateBookmark(entity) }
				bookmarkDao.getBookMarkFromBookMarkId(id.await())
			}
			result?.let {
				Resource.Success(data = it.toModel())
			} ?: Resource.Error(Exception("Invalid bookmark id"))

		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, e.message ?: "")
		}
	}

	override suspend fun deleteBookMarks(bookmarks: Collection<AudioBookmarkModel>): Resource<Unit, Exception> {
		return try {
			val entities = bookmarks.map { it.toEntity() }

			withContext(Dispatchers.IO) {
				bookmarkDao.deleteBookMarks(entities)
			}
			Resource.Success(Unit)
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, e.message ?: "")
		}
	}
}