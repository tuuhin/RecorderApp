package com.eva.recordings.domain.tasks

fun interface RemoveTrashRecordingTask {

	suspend fun invoke(): Result<Unit>
}