package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.eva.recorderapp.voice_recorder.domain.models.AudioFileModel
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.ContentLoadState

@Composable
fun ContentStateLoading(
	loadState: ContentLoadState,
	onSuccess: @Composable BoxScope.(AudioFileModel) -> Unit,
	modifier: Modifier = Modifier,
	animationSpec: FiniteAnimationSpec<Float> = tween(durationMillis = 200, easing = EaseInOut)
) {
	Crossfade(
		targetState = loadState,
		animationSpec = animationSpec,
		modifier = modifier
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