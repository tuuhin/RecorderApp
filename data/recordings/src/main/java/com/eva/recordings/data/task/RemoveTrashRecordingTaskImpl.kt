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
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

private const val TAG = "REMOVE_TRASH_RECORDING_TASK"

class RemoveTrashRecordingTaskImpl(val provider: TrashRecordingsProvider) :
	RemoveTrashRecordingTask {

	override suspend fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
		// no need to do anything for Api 30 as removing trash files is handled by the system
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return@withContext Result.success(Unit)
		// if its api 29 then handle removing items via this class
		when (val resource = provider.getTrashedVoiceRecordings()) {
			is Resource.Success -> {
				val trashModels = evaluateItemsToRemove(resource.data)
				Log.d(TAG, "READY TO REMOVE :${trashModels.size} MODELS")
				when (val res = provider.permanentlyDeleteRecordingsInTrash(trashModels)) {
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

			is Resource.Error -> {
				val errorMessage = "DELETE FAILED :${resource.error.message ?: "UNKNOWN"}"
				Log.d(TAG, "ERROR OCCCURED :$errorMessage ")
				Result.failure(resource.error)
			}
			// it's an invalid state so no need to check or attach some message
			Resource.Loading -> Result.failure(Exception("Unwanted state"))
		}
	}

	@OptIn(ExperimentalTime::class)
	private fun evaluateItemsToRemove(items: List<TrashRecordingModel>): Set<TrashRecordingModel> {
		val currentInstant = Clock.System.now()
		// delete-now delete is always at future so
		// delete-now is always positive except the case of
		// expires then delete-now is negative
		val expiredByTime = items.filter { model ->
			val deleteInstant = model.expiresAt.toInstant(TimeZone.currentSystemDefault())
			val diff = deleteInstant.minus(currentInstant)
			diff.toLong(DurationUnit.MINUTES) < 0
		}
		// if mistakenly some files are deleted but the metadata already exits then delete them too.
		val expiredByDelete = items.filter { model ->
			val fileUri = model.fileUri.toUri()
			if (fileUri.scheme == "file") return@filter false
			val file = fileUri.toFile()
			// take if file and file don't exit or file is empty
			file.isFile && (!file.exists() || file.length() != 0L)
		}

		val trashModels = expiredByDelete union expiredByTime
		Log.d(TAG, "NO. OF TO BE  FILES DELETED :${trashModels.size} ")
		return trashModels
	}
}
