package com.eva.recorderapp.voice_recorder.data.worker

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.eva.recorderapp.voice_recorder.data.recordings.provider.RecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingsProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.time.Duration

@HiltWorker
class UpdateRecordingPathWorker @AssistedInject constructor(
	@Assisted private val context: Context,
	@Assisted private val params: WorkerParameters,
	private val recordingsProvider: VoiceRecordingsProvider,
) : CoroutineWorker(context, params) {

	private val isFreshInstall: Boolean
		get() = context.packageManager.getPackageInfo(context.packageName, 0).let {
			it.firstInstallTime == it.lastUpdateTime
		}

	override suspend fun doWork(): Result {
		// no need to update the path if it's a fresh install or api31 below
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || isFreshInstall) return Result.success()
		// recording path is available from API31+
		return withContext(Dispatchers.IO) {
			try {
				val urisCreatedByThisApp = recordingsProvider.getVoiceRecordings()
					.filter { it.owner == context.packageName }
					.map { it.fileUri.toUri() }
				// if there is none everything is good
				if (urisCreatedByThisApp.isEmpty()) {
					return@withContext Result.success()
				}
				// update it to the new path
				val updatedMetadata = ContentValues().apply {
					put(
						MediaStore.Audio.AudioColumns.RELATIVE_PATH,
						RecordingsProvider.RECORDINGS_MUSIC_PATH
					)
				}
				val operations = urisCreatedByThisApp.map { uri ->
					async { context.contentResolver.update(uri, updatedMetadata, null) }
				}
				operations.awaitAll()
				Result.success(
					workDataOf(
						WorkerParams.UPDATE_RECORDING_PATH_SUCCESS_KEY to "EXECUTED SUCCESSFULLY"
					)
				)
			} catch (e: Exception) {
				e.printStackTrace()
				val message = e.message ?: "SOME ERROR OCCURRED"
				Result.failure(
					workDataOf(WorkerParams.UPDATE_RECORDING_PATH_FAILED_KEY to message)
				)
			}
		}
	}

	companion object {

		private const val UNIQUE_NAME = "UPDATE_RECORDING_PATH_WORKER"

		private val workRequest = OneTimeWorkRequestBuilder<UpdateRecordingPathWorker>()
			.setInitialDelay(Duration.ofSeconds(5))
			.build()

		fun startWorker(context: Context) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

			val workManager = WorkManager.getInstance(context)

			workManager.enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, workRequest)
		}

		fun cancelWork(context: Context) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

			//cancel worker only for android 10
			val workManager = WorkManager.getInstance(context)
			workManager.cancelUniqueWork(UNIQUE_NAME)
		}

	}
}