package com.eva.recorderapp.voice_recorder.data.recorder

import android.database.sqlite.SQLiteException
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.data.database.dao.RecordingsBookmarkDao
import com.eva.recorderapp.voice_recorder.data.database.entity.RecordingBookMarkEntity
import com.eva.recorderapp.voice_recorder.domain.recorder.CreateRecordingBookmarkRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime

class CreateRecordingBookmarkRepoImpl(
	private val bookmarkDao: RecordingsBookmarkDao,
) : CreateRecordingBookmarkRepo {

	override suspend fun createBookMarks(recordingId: Long, points: Collection<LocalTime>)
			: Resource<Unit, Exception> {
		val entities = points.map { point ->
			RecordingBookMarkEntity(recordingId = recordingId, timeStamp = point)
		}
		return try {
			withContext(Dispatchers.IO) {
				bookmarkDao.addBookMarks(entities)
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