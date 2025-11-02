package com.eva.player_shared.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.EaseInBounce
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eva.ui.R
import com.eva.ui.theme.CustomShapes
import com.eva.ui.theme.RecorderAppTheme
import com.eva.ui.theme.RoundedPolygonShape

@Composable
fun AnimatedPlayPauseButton(
	isPlaying: Boolean,
	onPause: () -> Unit,
	onPlay: () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	tonalElevation: Dp = 0.dp,
	shadowElevation: Dp = 0.dp,
	containerColor: Color = MaterialTheme.colorScheme.primary,
	disabledColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = .1f)
) {

	val buttonContainerColor = if (enabled) containerColor else disabledColor

	val infiniteTransition = rememberInfiniteTransition(
		label = "Infinite transition for rotating shape"
	)

	val rotation by infiniteTransition.animateFloat(
		initialValue = 0f,
		targetValue = if (isPlaying) 360f else 0f,
		animationSpec = infiniteRepeatable(
			animation = tween(8_000, easing = LinearEasing),
			repeatMode = RepeatMode.Restart
		),
		label = "Amount of rotation for the play button",
	)

	Surface(
		onClick = { if (isPlaying) onPause() else onPlay() },
		enabled = enabled,
		contentColor = contentColorFor(buttonContainerColor),
		color = buttonContainerColor,
		modifier = modifier.graphicsLayer {
			clip = true
			shape = RoundedPolygonShape(
				polygon = CustomShapes.ROUNDED_STAR_8_CORNERS,
				rotation = rotation
			)
		},
		tonalElevation = tonalElevation,
		shadowElevation = shadowElevation,
	) {
		Box(
			modifier = Modifier.sizeIn(
				minWidth = dimensionResource(id = R.dimen.play_button_min_size),
				minHeight = dimensionResource(id = R.dimen.play_button_min_size)
			),
			contentAlignment = Alignment.Center,
		) {
			AnimatedContent(
				targetState = isPlaying,
				transitionSpec = { isPlayingAnimation() },
				label = "Transform between playing states",
				contentAlignment = Alignment.Center,
			) { playing ->
				if (playing)
					Icon(
						painter = painterResource(id = R.drawable.ic_pause),
						contentDescription = stringResource(R.string.recorder_action_pause),
						modifier = Modifier.size(32.dp),
					)
				else Icon(
					painter = painterResource(id = R.drawable.ic_play),
					contentDescription = stringResource(R.string.recorder_action_resume),
					modifier = Modifier.size(32.dp),
				)
			}
		}
	}
}

private fun isPlayingAnimation(): ContentTransform {

	val fadeSpec = tween<Float>(durationMillis = 200, delayMillis = 100, easing = EaseInBounce)
	val scaleSpec = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy)

	val enter =
		fadeIn(animationSpec = fadeSpec, initialAlpha = .2f) +
				scaleIn(animationSpec = scaleSpec, initialScale = .4f)
	val exit = fadeOut(animationSpec = fadeSpec) + scaleOut(animationSpec = scaleSpec)

	return enter togetherWith exit

}

private class BooleanPreviewParams : CollectionPreviewParameterProvider<Boolean>(
	listOf(true, false)
)


@PreviewLightDark
@Composable
private fun AnimatedPlayPauseButtonIsPlayingPreview(
	@PreviewParameter(BooleanPreviewParams::class)
	isPlaying: Boolean,
) = RecorderAppTheme {
	Surface {
		AnimatedPlayPauseButton(
			isPlaying = isPlaying,
			onPause = { },
			onPlay = { },
		)
	}
}