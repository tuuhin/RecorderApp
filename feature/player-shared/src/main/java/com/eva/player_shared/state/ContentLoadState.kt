package com.eva.player_shared.state

import androidx.compose.runtime.Stable

@Stable
sealed class ContentLoadState<out T> {

	data object Loading : ContentLoadState<Nothing>()

	data class Content<T>(val data: T) : ContentLoadState<T>()

	data object Unknown : ContentLoadState<Nothing>()
}