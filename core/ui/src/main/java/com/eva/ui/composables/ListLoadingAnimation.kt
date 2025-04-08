package com.eva.ui.composables

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList

@Composable
fun <T> ListLoadingAnimation(
	isLoaded: Boolean,
	items: ImmutableList<T>,
	onDataReady: @Composable (ImmutableList<T>) -> Unit,
	onNoItems: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	onLoading: (@Composable () -> Unit)? = null,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	animationSpec: FiniteAnimationSpec<Float> = tween(durationMillis = 600, easing = EaseInOut),
) {

	val listInfoState by remember(isLoaded, items) {
		derivedStateOf {
			when {
				!isLoaded -> ListInformationState.LOADING
				items.isNotEmpty() -> ListInformationState.DATA
				else -> ListInformationState.EMPTY
			}
		}
	}

	Crossfade(
		targetState = listInfoState,
		animationSpec = animationSpec,
		label = "List loading cross-fade animation",
		modifier = modifier.padding(contentPadding),
	) { state ->
		when (state) {
			ListInformationState.LOADING -> onLoading?.invoke()
				?: run {
					Box(
						modifier = Modifier.fillMaxSize(),
						contentAlignment = Alignment.Center,
					) {
						CircularProgressIndicator()
					}
				}

			ListInformationState.EMPTY -> onNoItems()
			ListInformationState.DATA -> onDataReady(items)
		}
	}
}

private enum class ListInformationState {
	LOADING,
	EMPTY,
	DATA
}