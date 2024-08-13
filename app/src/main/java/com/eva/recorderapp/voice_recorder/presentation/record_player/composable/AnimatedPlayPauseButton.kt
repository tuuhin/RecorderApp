package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.ui.theme.RoundedPolygonShape

@Composable
fun AnimatedPlayPauseButton(
	isPlaying: Boolean,
	onPause: () -> Unit,
	onPlay: () -> Unit,
	modifier: Modifier = Modifier,
	containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
	contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
	// TODO: Check recomposition

	val polygon = remember {
		RoundedPolygon.star(
			numVerticesPerRadius = 8,
			innerRadius = .4f,
			radius = 0.8f,
			rounding = CornerRounding(radius = 0.2f)
		)
	}

	val infiniteTrasition = rememberInfiniteTransition()

	val rotation by infiniteTrasition.animateFloat(
		initialValue = 0f,
		targetValue = 360f,
		animationSpec = infiniteRepeatable(
			animation = tween(10_000, easing = LinearEasing),
			repeatMode = RepeatMode.Restart
		)
	)

	val clipShape = remember(polygon, isPlaying, rotation) {
		RoundedPolygonShape(polygon, if (isPlaying) rotation else 0f)
	}

	FloatingActionButton(
		onClick = {
			if (isPlaying) onPause()
			else onPlay()
		},
		contentColor = contentColor,
		containerColor = containerColor,
		modifier = modifier
			.sizeIn(minWidth = 72.dp, minHeight = 72.dp)
			.clip(clipShape)
			.graphicsLayer {
				shadowElevation = 4.dp.toPx()
			},
	) {
		AnimatedContent(
			targetState = isPlaying,
			transitionSpec = { isPlayingAnimation() },
			label = "Transform between playing states",
			contentAlignment = Alignment.Center,
			modifier = Modifier.padding(8.dp)
		) { playing ->
			if (playing)
				Icon(
					painter = painterResource(id = R.drawable.ic_pause),
					contentDescription = stringResource(R.string.recorder_action_pause),
				)
			else Icon(
				painter = painterResource(id = R.drawable.ic_play),
				contentDescription = stringResource(R.string.recorder_action_resume),
			)
		}
	}
}

private fun AnimatedContentTransitionScope<Boolean>.isPlayingAnimation(): ContentTransform {
	return fadeIn(
		animationSpec = tween(200)
	) + scaleIn(
		animationSpec = spring(
			dampingRatio = Spring.DampingRatioLowBouncy,
			stiffness = Spring.StiffnessLow,
		),
	) togetherWith fadeOut(
		animationSpec = tween(200)
	) + scaleOut(
		animationSpec = spring(
			dampingRatio = Spring.DampingRatioLowBouncy,
			stiffness = Spring.StiffnessLow,
		),
	)
}

class IsPlayingPreviewParams
	: CollectionPreviewParameterProvider<Boolean>(listOf(true, false))


@PreviewLightDark
@Composable
private fun AnimatedPlayPauseButtonIsPlayingPreview(
	@PreviewParameter(IsPlayingPreviewParams::class)
	isPlaying: Boolean
) = RecorderAppTheme {
	AnimatedPlayPauseButton(
		isPlaying = isPlaying,
		onPause = { },
		onPlay = { },
	)
}

