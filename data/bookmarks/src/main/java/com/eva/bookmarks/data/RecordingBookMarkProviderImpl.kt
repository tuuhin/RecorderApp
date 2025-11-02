package com.eva.bookmarks.data

import android.database.sqlite.SQLiteException
import com.eva.bookmarks.domain.AudioBookmarkModel
import com.eva.bookmarks.domain.exceptions.InvalidBookMarkIdException
import com.eva.bookmarks.domain.provider.RecordingBookmarksProvider
import com.eva.database.dao.RecordingsBookmarkDao
import com.eva.database.entity.RecordingBookMarkEntity
import com.eva.recordings.domain.exceptions.InvalidRecordingIdException
import com.eva.recordings.domain.provider.RecordingsSecondaryDataProvider
import com.eva.utils.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime

internal class RecordingBookMarkProviderImpl(
	private val bookmarkDao: RecordingsBookmarkDao,
	private val provider: RecordingsSecondaryDataProvider,
) : RecordingBookmarksProvider {

	override fun getRecordingBookmarksFromId(audioId: Long): Flow<List<AudioBookmarkModel>> {
		return bookmarkDao.getBookMarksFromRecordingIdAsFlow(audioId)
			.map { entities -> entities.map { it.toModel() } }
			.flowOn(Dispatchers.IO)
	}

	override suspend fun getRecordingBookmarksFromIdAsList(audioId: Long): List<AudioBookmarkModel> {
		return bookmarkDao.getBookMarksFromRecordingId(audioId)
			.map { it.toModel() }
	}

	override suspend fun createBookMarks(
		recordingId: Long,
		points: Collection<LocalTime>
	): Resource<Unit, Exception> {
		val entities = points.map { point ->
			RecordingBookMarkEntity(recordingId = recordingId, timeStamp = point)
		}
		return try {
			// check if metadata exists
			val isPresent = provider.checkRecordingIdExists(recordingId) ?: false
			if (!isPresent) {
				val result = provider.insertRecordingMetaData(recordingId)
				if (result is Resource.Error)
					return Resource.Error(error = result.error, message = result.message)
			}
			bookmarkDao.insertOrUpdateBookmarks(entities)

			Resource.Success(Unit)
		} catch (e: CancellationException) {
			throw e
		} catch (e: SQLiteException) {
			e.printStackTrace()
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, e.message ?: "")
		}
	}

	override suspend fun updateBookMark(bookmark: AudioBookmarkModel, text: String?)
			: Resource<AudioBookmarkModel, Exception> {
		return try {
			// check if its present
			val isPresent = provider.checkRecordingIdExists(bookmark.recordingId) ?: false
			if (!isPresent) return Resource.Error(InvalidRecordingIdException())
			// create or update the bookmark
			val entity = bookmark.toEntity().copy(text = text ?: "")
			val newId = bookmarkDao.insertOrUpdateBookmark(entity)
			val bookMarkId = if (newId == -1L) bookmark.bookMarkId else newId
			// then get it
			val updatedEntity = bookmarkDao.getBookMarkFromBookMarkId(bookMarkId)
				?: return Resource.Error(InvalidBookMarkIdException())

			Resource.Success(data = updatedEntity.toModel())
		} catch (e: CancellationException) {
			throw e
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, e.message ?: "")
		}
	}

	override suspend fun createBookMark(recordingId: Long, time: LocalTime, text: String)
			: Resource<AudioBookmarkModel, Exception> {
		return try {
			val isPresent = provider.checkRecordingIdExists(recordingId) ?: false
			if (!isPresent) {
				// insert the metadata
				val result = provider.insertRecordingMetaData(recordingId)
				if (result is Resource.Error)
					return Resource.Error(error = result.error, message = result.message)
			}
			// if recording id not exists
			val entity = RecordingBookMarkEntity(
				text = text,
				recordingId = recordingId,
				timeStamp = time
			)
			val id = bookmarkDao.insertOrUpdateBookmark(entity)
			val updatedEntity = bookmarkDao.getBookMarkFromBookMarkId(id)
				?: return Resource.Error(InvalidBookMarkIdException())
			Resource.Success(data = updatedEntity.toModel())
		} catch (e: CancellationException) {
			throw e
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
			bookmarkDao.deleteBookMarks(entities)
			Resource.Success(Unit)
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, e.message ?: "")
		}
	}
}