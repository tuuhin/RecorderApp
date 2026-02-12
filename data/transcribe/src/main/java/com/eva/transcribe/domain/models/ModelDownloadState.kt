package com.eva.transcribe.domain.models

sealed class ModelDownloadState {
	data object DownloadInitiated : ModelDownloadState()
	data class DownloadProgress(val progress: Int) : ModelDownloadState()
	data object ModelUnzipping : ModelDownloadState()
	data object ModelReadyToSave : ModelDownloadState()
	data object ModelSaved : ModelDownloadState()
	data object DownloadCleanUpDone : ModelDownloadState()
}