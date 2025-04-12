package com.eva.recordings.data.task

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import com.eva.recordings.data.wrapper.RecordingsConstants
import com.eva.recordings.domain.provider.VoiceRecordingsProvider
import com.eva.recordings.domain.tasks.RecordingPathCorrectionTask
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal class RecordingsPathCorrectionTaskImpl(
	private val context: Context,
	private val recordingsProvider: VoiceRecordingsProvider,
) : RecordingPathCorrectionTask {

	override suspend fun invoke(): Result<Unit> {
		// This worker is added to rectify some earlier mistake with the location of the recordings
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return Result.success(Unit)
		// otherwise correct the path if any is left.
		return coroutineScope {
			try {
				val ownerUris = recordingsProvider.getVoiceRecordings()
					.filter { it.owner == context.packageName }
					.map { it.fileUri.toUri() }
				// if there is none everything is good
				if (ownerUris.isEmpty()) return@coroutineScope Result.success(Unit)

				// update it to the new path
				val updatedMetadata = ContentValues().apply {
					put(
						MediaStore.Audio.AudioColumns.RELATIVE_PATH,
						RecordingsConstants.RECORDINGS_MUSIC_PATH
					)
				}

				val operations = ownerUris.map { uri ->
					async { context.contentResolver.update(uri, updatedMetadata, null) }
				}
				operations.awaitAll()
				Result.success(Unit)
			} catch (e: Exception) {
				Result.failure(e)
			}
		}
	}
}