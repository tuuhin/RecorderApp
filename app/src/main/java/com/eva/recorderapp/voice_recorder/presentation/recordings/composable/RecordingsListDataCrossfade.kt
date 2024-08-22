package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.ListInformationState
import kotlinx.collections.immutable.ImmutableList

@Composable
fun <T> RecordingsListDataCrossfade(
	isRecordingsLoaded: Boolean,
	recordings: ImmutableList<T>,
	onData: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	isBin: Boolean = false,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	animationSpec: FiniteAnimationSpec<Float> = tween(durationMillis = 600, easing = EaseInOut)
) {

	val listInfoState by remember(isRecordingsLoaded, recordings) {
		derivedStateOf {
			when {
				!isRecordingsLoaded -> ListInformationState.LOADING
				recordings.isNotEmpty() -> ListInformationState.DATA
				else -> ListInformationState.EMPTY
			}
		}
	}

	Crossfade(
		targetState = listInfoState,
		animationSpec = animationSpec,
		modifier = modifier.padding(contentPadding),
	) { state ->
		when (state) {
			ListInformationState.LOADING -> {
				Box(
					modifier = Modifier.fillMaxSize(),
					contentAlignment = Alignment.Center,
				) {
					CircularProgressIndicator()
				}
			}

			ListInformationState.EMPTY -> {
				Column(
					modifier = Modifier.fillMaxSize(),
					verticalArrangement = Arrangement.Center,
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					val binPainter = if (isBin) painterResource(id = R.drawable.ic_bin)
					else painterResource(id = R.drawable.ic_recorder)
					Image(
						painter = binPainter,
						contentDescription = stringResource(R.string.no_recodings),
						colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.secondary)
					)
					Spacer(modifier = Modifier.height(20.dp))
					Text(
						text = stringResource(id = R.string.no_recodings),
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.tertiary
					)
				}
			}

			ListInformationState.DATA -> onData()
		}

	}
}