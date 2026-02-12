package com.eva.worker

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.eva.transcribe.domain.ModelDownloadManager
import com.eva.transcribe.domain.models.ModelDownloadState
import com.eva.utils.NotificationConstants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
internal class STTModelDownloaderWorker @AssistedInject constructor(
	@Assisted private val context: Context,
	@Assisted private val params: WorkerParameters,
	private val task: ModelDownloadManager,
) : CoroutineWorker(context, params) {

	override suspend fun doWork(): Result {
		val modelId = params.inputData.getString(WorkerParams.WORK_DATA_STT_MODEL_ID)
			?: return Result.failure(
				workDataOf(
					WorkerParams.WORK_DATA_SST_MODEL_WORK_FAILED to
							WorkerParams.WORK_DATA_SST_MODEL_ID_MISSING
				)
			)

		val taskResult = task.downloadAndSaveModel(
			modelId = modelId,
			onDownloadState = { state -> setForegroundAsync(createNotification(state)) },
		)
		if (taskResult.isFailure) {
			val reason = taskResult.exceptionOrNull()?.message ?: "Some unwanted issue happened"
			return Result.failure(workDataOf(WorkerParams.WORK_DATA_SST_MODEL_WORK_FAILED to reason))
		}

		val isSuccess = taskResult.getOrNull() ?: false
		return Result.success(workDataOf(WorkerParams.WORK_DATA_SST_MODEL_WORK_SUCCESS to isSuccess))
	}


	private fun createNotification(state: ModelDownloadState): ForegroundInfo {
		val title = applicationContext.getString(R.string.stt_model_download_notification_text)

		// TODO: Check cancellation
		// This PendingIntent can be used to cancel the worker
		val intent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

		val text = when (state) {
			is ModelDownloadState.DownloadProgress -> applicationContext.getString(R.string.stt_model_downloading_text)
			ModelDownloadState.DownloadInitiated -> applicationContext.getString(R.string.stt_model_download_requested_text)
			ModelDownloadState.ModelUnzipping -> applicationContext.getString(R.string.stt_model_unzip_text)
			ModelDownloadState.ModelReadyToSave -> applicationContext.getString(R.string.stt_model_ready_text)
			ModelDownloadState.ModelSaved -> applicationContext.getString(R.string.stt_model_saved_text)
			ModelDownloadState.DownloadCleanUpDone -> applicationContext.getString(R.string.stt_model_cleanup_text)
		}

		// set the progress message
		setProgressAsync(workDataOf(WorkerParams.WORK_DATA_WORK_PROGRESS_MESSAGE to text))
		if (state is ModelDownloadState.DownloadProgress) {
			setProgressAsync(workDataOf(WorkerParams.WORK_DATA_WORK_PROGRESS_PERCENTAGE to state.progress))
		}

		val notificationAction = NotificationCompat.Action.Builder(
			IconCompat.createWithResource(applicationContext, R.drawable.ic_cancel),
			"Cancel",
			intent
		).build()

		val notification = NotificationCompat
			.Builder(applicationContext, NotificationConstants.WORKER_CHANNEL_ID)
			.setContentTitle(title)
			.setContentText(text)
			.setSmallIcon(R.drawable.ic_model_download)
			.setOngoing(true)
			.addAction(notificationAction).apply {
				when (state) {
					is ModelDownloadState.DownloadProgress ->
						setProgress(100, state.progress, false)

					ModelDownloadState.ModelUnzipping -> setProgress(100, 0, true)
					else -> setProgress(0, 0, false)
				}
			}
			.build()

		return ForegroundInfo(
			NotificationConstants.DOWNLOAD_STT_MODEL_WORKER_NOTIFICATION_ID,
			notification,
			ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
		)
	}
}