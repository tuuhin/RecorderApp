package com.eva.worker.domain

sealed interface WorkResource {
	data object Enqueued : WorkResource
	data class Running(val message: String, val progress: Int? = null) : WorkResource
	data class Success(val message: String? = null) : WorkResource
	data class Failed(val reason: String, val error: Throwable? = null) : WorkResource
	data class Canceled(val reason: String) : WorkResource
	data object Blocked : WorkResource
}