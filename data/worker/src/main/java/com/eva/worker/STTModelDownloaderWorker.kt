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
			?: return Result.failure(workDataOf(WorkerParams.WORK_DATA_SST_MODEL_WORK_FAILED to "Required model id is not passed"))

		val re = task.downloadAndSaveModel(
			modelId = modelId,
			onDownloadState = { state -> setForegroundAsync(createNotification(state)) },
		)
		return if (re.isSuccess) {
			val isSuccess = re.getOrNull() ?: false
			Result.success(workDataOf(WorkerParams.WORK_DATA_SST_MODEL_WORK_SUCCESS to isSuccess))
		} else {
			val reason = re.exceptionOrNull()?.message ?: "Some unwanted issue happened"
			Result.failure(workDataOf(WorkerParams.WORK_DATA_SST_MODEL_WORK_FAILED to reason))
		}
	}


	private fun createNotification(state: ModelDownloadState): ForegroundInfo {
		val title = applicationContext.getString(R.string.stt_model_download_notification_text)
		// This PendingIntent can be used to cancel the worker
		val intent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

		val notificationText = when (state) {
			is ModelDownloadState.DownloadProgress -> applicationContext.getString(R.string.stt_model_downloading_text)
			ModelDownloadState.DownloadRequested -> applicationContext.getString(R.string.stt_model_download_requested_text)
			ModelDownloadState.ModelUnzipping -> applicationContext.getString(R.string.stt_model_unzip_text)
			ModelDownloadState.ModelSaved -> applicationContext.getString(R.string.stt_model_ready_text)
		}

		val notificationAction = NotificationCompat.Action.Builder(
			IconCompat.createWithResource(applicationContext, R.drawable.ic_cancel),
			"Cancel",
			intent
		).setShowsUserInterface(false)
			.build()

		val notification = NotificationCompat.Builder(
			applicationContext,
			NotificationConstants.WORKER_CHANNEL_ID
		)
			.setContentTitle(title)
			.setContentText(notificationText)
			.setSmallIcon(R.drawable.ic_model_download)
			.setOngoing(true)
			.addAction(notificationAction).apply {
				when (state) {
					is ModelDownloadState.DownloadProgress -> setProgress(
						100,
						state.progress.toInt(),
						false
					)

					ModelDownloadState.ModelUnzipping -> setProgress(0, 0, true)
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