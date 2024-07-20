package com.eva.recorderapp.common

sealed class Resource<out S, out E> {

	data class Success<out S, out E>(
		val data: S,
		val message: String? = null
	) : Resource<S, Nothing>()

	data class Error<out S, out E : Exception>(
		val error: E,
		val message: String? = null
	) : Resource<Nothing, E>()

	data object Loading : Resource<Nothing, Nothing>()
}