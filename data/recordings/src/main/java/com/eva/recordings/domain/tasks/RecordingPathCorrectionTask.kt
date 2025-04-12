package com.eva.recordings.domain.tasks

fun interface RecordingPathCorrectionTask {

	suspend fun invoke(): Result<Unit>
}