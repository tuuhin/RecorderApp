package com.eva.player_shared.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.eva.player_shared.state.ContentLoadState
import com.eva.player_shared.util.AudioFileModelLoadState
import com.eva.player_shared.util.PlayerPreviewFakes

@Composable
fun <T> ContentStateAnimatedContainer(
	loadState: ContentLoadState<T>,
	onSuccess: @Composable BoxScope.(T) -> Unit,
	onFailed: @Composable BoxScope.() -> Unit,
	modifier: Modifier = Modifier,
	onLoading: (@Composable BoxScope.() -> Unit)? = null,
) {

	// This wrapper is needed as if T value changes it will lead to a recomposition
	val plainState by remember(loadState) {
		derivedStateOf {
			when (loadState) {
				is ContentLoadState.Content -> PlainContentState.IS_SUCCESS
				ContentLoadState.Loading -> PlainContentState.IS_SUCCESS
				ContentLoadState.Unknown -> PlainContentState.IS_ERROR
			}
		}
	}

	AnimatedContent(
		targetState = plainState,
		transitionSpec = { animateLoadState() },
		label = "Animating content state",
		contentAlignment = Alignment.Center,
		modifier = modifier.fillMaxSize(),
	) { state ->
		Box(
			modifier = Modifier.fillMaxSize()
		) {
			when (state) {
				PlainContentState.IS_LOADING -> onLoading?.invoke(this)
					?: CircularProgressIndicator(
						modifier = Modifier.align(Alignment.Center)
					)

				PlainContentState.IS_SUCCESS ->
					(loadState as? ContentLoadState.Content)?.data?.let { onSuccess(it) }

				PlainContentState.IS_ERROR -> onFailed()
			}
		}
	}
}

// enums to represent the state
private enum class PlainContentState {
	IS_LOADING,
	IS_SUCCESS,
	IS_ERROR,
}

private fun AnimatedContentTransitionScope<PlainContentState>.animateLoadState(): ContentTransform {
	val loadContentTransition: FiniteAnimationSpec<Float> = tween(
		durationMillis = 800,
		easing = FastOutSlowInEasing
	)

	val normalTransition: FiniteAnimationSpec<Float> = tween(
		durationMillis = 200,
		delayMillis = 60,
		easing = FastOutLinearInEasing
	)

	return if (initialState == PlainContentState.IS_LOADING && targetState == PlainContentState.IS_SUCCESS) {
		fadeIn(animationSpec = loadContentTransition) + expandVertically(
			animationSpec = spring(
				dampingRatio = Spring.DampingRatioLowBouncy,
				stiffness = Spring.StiffnessLow
			),
			expandFrom = Alignment.CenterVertically,
		) togetherWith
				fadeOut(loadContentTransition) + shrinkVertically(
			animationSpec = spring(
				dampingRatio = Spring.DampingRatioLowBouncy,
				stiffness = Spring.StiffnessLow
			),
			shrinkTowards = Alignment.CenterVertically,
		)
	} else fadeIn(normalTransition) togetherWith fadeOut(normalTransition)
}

class ContentLoadStatePreviewParams : CollectionPreviewParameterProvider<AudioFileModelLoadState>(
	listOf(
		ContentLoadState.Loading,
		ContentLoadState.Content(PlayerPreviewFakes.FAKE_AUDIO_MODEL),
		ContentLoadState.Unknown
	),
)
