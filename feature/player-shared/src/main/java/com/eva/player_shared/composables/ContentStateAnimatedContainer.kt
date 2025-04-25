package com.eva.player_shared.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
	AnimatedContent(
		targetState = loadState,
		transitionSpec = { animateLoadState() },
		label = "Animating content state",
		contentAlignment = Alignment.Center,
		modifier = modifier.fillMaxSize(),
	) { state ->
		Box(
			modifier = Modifier.fillMaxSize()
		) {
			when (state) {
				is ContentLoadState.Content -> onSuccess(state.data)

				ContentLoadState.Loading -> onLoading?.invoke(this) ?: CircularProgressIndicator(
					modifier = Modifier.align(Alignment.Center)
				)

				ContentLoadState.Unknown -> onFailed()
			}
		}
	}
}

private fun <T> AnimatedContentTransitionScope<ContentLoadState<T>>.animateLoadState(
	loadContentTransition: FiniteAnimationSpec<Float> = tween(
		durationMillis = 800,
		easing = FastOutSlowInEasing
	),
	normalTransition: FiniteAnimationSpec<Float> = tween(
		durationMillis = 200,
		delayMillis = 60,
		easing = FastOutLinearInEasing
	)
): ContentTransform {

	// no transition is this case
	if (initialState is ContentLoadState.Content && targetState is ContentLoadState.Content) {
		return ContentTransform(
			targetContentEnter = EnterTransition.None,
			initialContentExit = ExitTransition.None,
		)
	}

	return if (initialState is ContentLoadState.Loading && targetState is ContentLoadState.Content) {
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
