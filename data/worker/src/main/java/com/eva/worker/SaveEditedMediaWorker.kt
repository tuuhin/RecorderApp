package com.eva.worker

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.eva.recordings.domain.tasks.SaveEditMediaItemTask
import com.eva.ui.R
import com.eva.utils.NotificationConstants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@HiltWorker
class SaveEditedMediaWorker @AssistedInject constructor(
	@Assisted private val context: Context,
	@Assisted private val params: WorkerParameters,
	private val task: SaveEditMediaItemTask
) : CoroutineWorker(context, params) {

	override suspend fun doWork(): Result {
		val inputData = params.inputData

		val fileUri = inputData.getString(WorkerParams.WORK_DATA_FILE_URI)
			?: return Result.failure(
				workDataOf(
					WorkerParams.WORK_DATA_REQUIRED_ITEMS_NOT_FOUND to
							WorkerParams.WORK_DATA_SAVE_EDITED_ITEM_FILE_URI_NOT_PROVIDED
				)
			)

		val fileName = inputData.getString(WorkerParams.WORK_DATA_FILE_NAME)
			?: return Result.failure(
				workDataOf(
					WorkerParams.WORK_DATA_REQUIRED_ITEMS_NOT_FOUND to
							WorkerParams.WORK_DATA_SAVE_EDITED_ITEM_FILE_NAME_NOT_PROVIDED
				)
			)

		val mimeType = inputData.getString(WorkerParams.WORK_DATA_FILE_MIME_TYPE)
			?: return Result.failure(
				workDataOf(
					WorkerParams.WORK_DATA_REQUIRED_ITEMS_NOT_FOUND to
							WorkerParams.WORK_DATA_SAVE_EDITED_ITEM_FILE_NAME_NOT_PROVIDED
				)
			)

		val file = File(fileUri)

		if (!file.exists()) return Result.failure(
			workDataOf(
				WorkerParams.WORK_DATA_REQUIRED_ITEMS_NOT_FOUND to
						WorkerParams.WORK_DATA_SAVE_EDITED_ITEM_FILE_INVALID
			)
		)

		val result = task.invoke(fileName, mimeType) { contentUri ->
			copyStreamData(
				inputFile = file,
				outputUri = contentUri.toUri(),
				onProgress = {
					val progress = (it * 100).toInt()
					setProgress(workDataOf(PROGRESS to progress))
					setForeground(getForegroundInfo(progress))
				},
			)
		}

		return if (result.isSuccess) {
			// after the file is saved on the disk delete the file
			withContext(Dispatchers.IO) { file.delete() }
			Result.success()
		} else {
			val exception = result.exceptionOrNull()
			val message = exception?.message ?: "Unknown"
			Result.failure(
				workDataOf(WorkerParams.WORK_SAVE_EDITED_ITEM_FAILED to message)
			)
		}
	}

	private fun getForegroundInfo(progress: Int): ForegroundInfo {
		val title = applicationContext.getString(R.string.save_edit_item_work_title)
		val text = applicationContext.getString(R.string.save_edit_item_work_text)

		val notification =
			Notification.Builder(applicationContext, NotificationConstants.RECORDING_CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_edit)
				.setOngoing(true)
				.setContentTitle(title)
				.setProgress(100, progress, false)
				.setContentText(text)
				.build()

		return ForegroundInfo(
			NotificationConstants.SAVE_EDITED_MEDIA_FOREGROUND_ID,
			notification,
			ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
		)
	}

	private suspend fun copyStreamData(
		inputFile: File,
		outputUri: Uri,
		bufferSize: Int = 2048,
		onProgress: suspend (Float) -> Unit = {}
	) {
		withContext(Dispatchers.IO) {
			context.contentResolver.openOutputStream(outputUri, "w")?.use { outputStream ->
				inputFile.inputStream().use { inputStream ->
					val totalBytes = inputFile.length()
					var bytesCopied = 0L
					val buffer = ByteArray(bufferSize)
					var bytesRead: Int
					do {
						bytesRead = inputStream.read(buffer)
						outputStream.write(buffer)
						bytesCopied += bytesRead
						onProgress(bytesCopied.toFloat() / totalBytes)
					} while (bytesRead >= 0)
				}
			} ?: Unit
		}
	}

	companion object {

		private const val PROGRESS = "PROGRESS"
		private const val TAG = "SAVE_EDIT_ITEM_WORKER"

		fun startWorkerAndObserve(
			context: Context,
			fileUri: String,
			fileName: String,
			mimeType: String,
		) {
			val constraints = Constraints.Builder()
				.setRequiresStorageNotLow(true)
				.build()

			val data = Data.Builder()
				.putString(WorkerParams.WORK_DATA_FILE_URI, fileUri)
				.putString(WorkerParams.WORK_DATA_FILE_NAME, fileName)
				.putString(WorkerParams.WORK_DATA_FILE_MIME_TYPE, mimeType)
				.build()

			val workRequest = OneTimeWorkRequestBuilder<SaveEditedMediaWorker>()
				.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
				.setInputData(data)
				.setConstraints(constraints)
				.build()

			val workManager = WorkManager.getInstance(context)
			workManager.enqueue(workRequest)
		}

		suspend fun observeWorkerState(
			context: Context,
			onProgress: (Int) -> Unit,
			onDone: () -> Unit,
			onFailed: (String?) -> Unit
		) {
			val workManager = WorkManager.getInstance(context)

			workManager.getWorkInfosByTagFlow(TAG)
				.collect { workInfo ->
					val info = workInfo.firstOrNull() ?: return@collect
					when (info.state) {
						WorkInfo.State.RUNNING -> {
							val progress = info.progress.getInt(PROGRESS, -1)
							if (progress != -1) onProgress(progress)
						}

						WorkInfo.State.SUCCEEDED -> onDone()
						WorkInfo.State.FAILED -> {
							val failedData =
								info.outputData.getString(WorkerParams.WORK_SAVE_EDITED_ITEM_FAILED)
							val invalidData =
								info.outputData.getString(WorkerParams.WORK_DATA_REQUIRED_ITEMS_NOT_FOUND)
							onFailed(failedData ?: invalidData)
						}

						else -> {}
					}
				}
		}
	}
}