package com.eva.transcribe.domain

import com.eva.transcribe.domain.models.ModelDownloadState

interface ModelDownloadManager {

	suspend fun downloadAndSaveModel(
		modelId: String,
		onDownloadState: suspend (ModelDownloadState) -> Unit,
	): Result<Boolean>
}