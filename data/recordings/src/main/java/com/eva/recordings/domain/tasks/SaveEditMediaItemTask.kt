package com.eva.recordings.domain.tasks

fun interface SaveEditMediaItemTask {

	suspend fun invoke(fileName: String, mimeType: String, operation: suspend (String) -> Unit)
			: Result<Unit>
}