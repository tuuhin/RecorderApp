package com.eva.recordings.data.task

import android.os.Build
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.eva.recordings.domain.models.TrashRecordingModel
import com.eva.recordings.domain.provider.TrashRecordingsProvider
import com.eva.recordings.domain.tasks.RemoveTrashRecordingTask
import com.eva.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

private const val TAG = "REMOVE_TRASH_RECORDING_TASK"

class RemoveTrashRecordingTaskImpl(val provider: TrashRecordingsProvider) :
	RemoveTrashRecordingTask {

	override suspend fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
		// no need to do anything for Api 30 as removing trash files is handled by the system
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return@withContext Result.success(Unit)
		// if its api 29 then handle removing items via this class
		val trashedFiles = provider.getTrashedVoiceRecordings() as? Resource.Success
			?: return@withContext Result.failure(Exception("Cannot fetch trash recordings"))

		val trashModels = evaluateItemsToRemove(trashedFiles.data).toList()
		Log.d(TAG, "READY TO REMOVE :${trashModels.size} MODELS")
		when (val res = provider.permanentlyDeleteRecordings(trashModels)) {
			is Resource.Error -> {
				val message = "DELETE FAILED :${res.error.message ?: "UNKNOWN"}"
				Log.e(TAG, message)
				return@withContext Result.failure(res.error)
			}

			is Resource.Success -> {
				val message = "FILES TO BE DELETED TODAY ${trashModels.size} "
				Log.d(TAG, message)
				Result.success(Unit)
			}

			else -> {}
		}
		Result.failure(Exception())
	}

	@OptIn(ExperimentalTime::class)
	private fun evaluateItemsToRemove(items: List<TrashRecordingModel>): List<TrashRecordingModel> {
		val currentInstant = Clock.System.now()
		// delete-now delete is always at future so
		// delete-now is always positive except the case of
		// expires then delete-now is negative
		val expiredByTime = items.filter { model ->
			val deleteInstant = model.expiresAt.toInstant(TimeZone.currentSystemDefault())
			val diff = deleteInstant.minus(currentInstant)
			// the difference is less than an hour
			diff < 1.hours
		}
		// if mistakenly some files are deleted but the metadata already exits then delete them too.
		val expiredByDelete = items.filter { model ->
			val fileUri = model.fileUri.toUri()
			val file = fileUri.toFile()
			if (!file.isFile) return@filter false
			// take if file and file don't exit or file is empty
			!file.exists() || file.length() != 0L
		}

		val trashModels = expiredByDelete union expiredByTime
		Log.d(TAG, "NO. OF TO BE  FILES DELETED :${trashModels.size} ")
		return trashModels.toList()
	}
}
