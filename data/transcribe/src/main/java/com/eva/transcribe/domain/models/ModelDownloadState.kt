package com.eva.transcribe.domain.models

sealed class ModelDownloadState {
	data object DownloadRequested : ModelDownloadState()
	data class DownloadProgress(val progress: Float) : ModelDownloadState()
	data object ModelUnzipping : ModelDownloadState()
	data object ModelSaved : ModelDownloadState()
}