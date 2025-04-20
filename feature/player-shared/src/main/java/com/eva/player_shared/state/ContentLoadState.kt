package com.eva.player_shared.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
sealed class ContentLoadState<T> {

	data object Loading : ContentLoadState<Nothing>()

	data class Content<T>(val data: T) : ContentLoadState<T>()

	data object Unknown : ContentLoadState<Nothing>()


	@Composable
	fun OnContentOrOther(content: @Composable (T) -> Unit, onOther: @Composable () -> Unit) {
		when (this) {
			is Content<T> -> content(data)
			else -> onOther()
		}
	}

	@Composable
	fun OnContent(action: @Composable (T) -> Unit) {
		if (this is Content<T>) action(this.data)
	}
}