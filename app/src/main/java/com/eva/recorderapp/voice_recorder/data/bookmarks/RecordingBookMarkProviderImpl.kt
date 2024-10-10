package com.eva.recorderapp.voice_recorder.data.bookmarks

import android.database.sqlite.SQLiteException
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.data.database.dao.RecordingsBookmarkDao
import com.eva.recorderapp.voice_recorder.data.database.entity.RecordingBookMarkEntity
import com.eva.recorderapp.voice_recorder.domain.bookmarks.AudioBookmarkModel
import com.eva.recorderapp.voice_recorder.domain.bookmarks.InvalidBookMarkIdException
import com.eva.recorderapp.voice_recorder.domain.bookmarks.RecordingBookmarksProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingsSecondaryDataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime

class RecordingBookMarkProviderImpl(
	private val bookmarkDao: RecordingsBookmarkDao,
	private val provider: RecordingsSecondaryDataProvider,
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
			// check if metadata exists
			withContext(Dispatchers.IO) {
				// get the current recording
				val isPresent = provider.checkRecordingIdExists(recordingId)
				if (isPresent != true) {
					val result = provider.insertRecordingMetaData(recordingId)
					if (result is Resource.Error)
						return@withContext Resource.Error<Unit, Exception>(
							error = result.error,
							message = result.message
						)
				}
				bookmarkDao.insertOrUpdateBookmarks(entities)
			}
			Resource.Success(Unit)
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
			val entity = bookmark.toEntity().copy(text = text ?: "")
			// get the current recording
			val result = withContext(Dispatchers.IO) {
				// create or update the bookmark
				bookmarkDao.insertOrUpdateBookmark(entity)
				// then get it
				bookmarkDao.getBookMarkFromBookMarkId(bookmark.bookMarkId)
			}
			result?.let { Resource.Success(data = it.toModel()) }
				?: Resource.Error(InvalidBookMarkIdException())
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
			withContext(Dispatchers.IO) {
				val isPresent = provider.checkRecordingIdExists(recordingId)
				if (isPresent != true) {
					// insert the metadata
					val result = provider.insertRecordingMetaData(recordingId)
					if (result is Resource.Error)
						return@withContext Resource.Error<AudioBookmarkModel, Exception>(
							error = result.error,
							message = result.message
						)
				}
				// if recording id not exists
				val entity = RecordingBookMarkEntity(
					text = text,
					recordingId = recordingId,
					timeStamp = time
				)
				val id = async { bookmarkDao.insertOrUpdateBookmark(entity) }
				bookmarkDao.getBookMarkFromBookMarkId(id.await())
					?.let { Resource.Success(data = it.toModel()) }
					?: Resource.Error(InvalidBookMarkIdException())
			}
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