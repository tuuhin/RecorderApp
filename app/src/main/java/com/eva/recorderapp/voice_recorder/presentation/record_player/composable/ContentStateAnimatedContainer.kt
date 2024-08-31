package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioFileModel
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.ContentLoadState

@Composable
fun ContentStateAnimatedContainer(
	loadState: ContentLoadState,
	onSuccess: @Composable BoxScope.(AudioFileModel) -> Unit,
	modifier: Modifier = Modifier,
) {
	AnimatedContent(
		targetState = loadState,
		modifier = modifier,
		transitionSpec = { animateLoadState() },
		contentAlignment = Alignment.Center
	) { state ->
		Box(
			modifier = Modifier.fillMaxSize()
		) {
			when (state) {
				is ContentLoadState.Content -> onSuccess(state.data)

				ContentLoadState.Loading -> CircularProgressIndicator(
					modifier = Modifier.align(Alignment.Center)
				)

				ContentLoadState.Unknown -> AudioFileNotFoundBox(
					modifier = Modifier.align(Alignment.Center)
				)
			}
		}
	}
}

private fun AnimatedContentTransitionScope<ContentLoadState>.animateLoadState(
	loadContentTransition: FiniteAnimationSpec<Float> = tween(
		durationMillis = 800,
		easing = FastOutSlowInEasing
	),
	normalTrasnsition: FiniteAnimationSpec<Float> = tween(
		durationMillis = 200,
		delayMillis = 60,
		easing = FastOutLinearInEasing
	)
): ContentTransform {
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
	} else fadeIn(normalTrasnsition) togetherWith fadeOut(normalTrasnsition)
}