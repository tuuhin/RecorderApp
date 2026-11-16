package com.eva.player_shared.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.eva.player_shared.state.ContentLoadState
import com.eva.player_shared.util.AudioFileModelLoadState
import com.eva.player_shared.util.PlayerPreviewFakes
import com.eva.ui.R

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
                ContentLoadState.Loading -> PlainContentState.IS_LOADING
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
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                PlainContentState.IS_LOADING -> onLoading?.invoke(this) ?: AudioContentLoading()

                PlainContentState.IS_SUCCESS -> (loadState as? ContentLoadState.Content)
                    ?.data?.let { onSuccess(it) }

                PlainContentState.IS_ERROR -> onFailed()
            }
        }
    }
}

@Composable
private fun AudioContentLoading(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.audio_content_preparing),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// enums to represent the state
private enum class PlainContentState {
    IS_LOADING,
    IS_SUCCESS,
    IS_ERROR,
}

private fun AnimatedContentTransitionScope<PlainContentState>.animateLoadState(
    transitionDuration: Int = 250,
    delayDuration: Int = 50,
): ContentTransform {
    return when (initialState) {
        PlainContentState.IS_LOADING if targetState == PlainContentState.IS_SUCCESS -> {
            fadeIn(
                animationSpec = tween(
                    durationMillis = transitionDuration,
                    delayMillis = delayDuration,
                ),
            ) + scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(
                    durationMillis = transitionDuration,
                    delayMillis = delayDuration,
                    easing = FastOutSlowInEasing,
                ),
            ) togetherWith fadeOut(animationSpec = tween(durationMillis = transitionDuration / 2))
        }

        PlainContentState.IS_LOADING if targetState == PlainContentState.IS_ERROR -> {
            slideInVertically(
                animationSpec = tween(
                    durationMillis = transitionDuration,
                    easing = FastOutSlowInEasing,
                ),
                initialOffsetY = { fullHeight -> -fullHeight / 4 },
            ) + fadeIn(
                animationSpec = tween(durationMillis = transitionDuration),
            ) togetherWith fadeOut(animationSpec = tween(durationMillis = transitionDuration / 2))
        }

        else -> fadeIn(animationSpec = tween(durationMillis = transitionDuration)) togetherWith
            fadeOut(animationSpec = tween(durationMillis = transitionDuration))
    }
}

class ContentLoadStatePreviewParams : CollectionPreviewParameterProvider<AudioFileModelLoadState>(
    listOf(
        ContentLoadState.Loading,
        ContentLoadState.Content(PlayerPreviewFakes.FAKE_AUDIO_MODEL),
        ContentLoadState.Unknown,
    ),
)
