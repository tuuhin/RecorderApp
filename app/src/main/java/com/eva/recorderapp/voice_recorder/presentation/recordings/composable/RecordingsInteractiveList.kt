package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.SelectableRecordings
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.collections.immutable.ImmutableList

@Composable
fun RecordingsInteractiveList(
	recordings: ImmutableList<SelectableRecordings>,
	onItemClick: (RecordedVoiceModel) -> Unit,
	onItemSelect: (RecordedVoiceModel) -> Unit,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp)
) {
	val isLocalInspectionMode = LocalInspectionMode.current

	val isAnySelected by remember(recordings) {
		derivedStateOf { recordings.any { it.isSelected } }
	}

	val keys: ((Int, SelectableRecordings) -> Any)? = remember {
		if (isLocalInspectionMode) null
		else { _, device -> device.recoding.id }
	}

	val contentType: ((Int, SelectableRecordings) -> Any?) = remember {
		{ _, _ -> RecordedVoiceModel::class.simpleName }
	}

	val isRecordingNotPresent by remember(recordings) {
		derivedStateOf { recordings.isEmpty() }
	}


	Crossfade(
		targetState = isRecordingNotPresent,
		modifier = modifier,
		animationSpec = tween(durationMillis = 200, easing = EaseInOut)
	) { isEMpty ->
		if (isEMpty) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(contentPadding),
				verticalArrangement = Arrangement.Center,
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Image(
					painter = painterResource(id = R.drawable.ic_recorder),
					contentDescription = stringResource(R.string.no_recodings),
					colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.secondary)
				)
				Spacer(modifier = Modifier.height(20.dp))
				Text(
					text = stringResource(id = R.string.no_recodings),
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onBackground
				)

			}

		} else LazyColumn(
			contentPadding = contentPadding,
			modifier = Modifier.fillMaxSize(),
			verticalArrangement = Arrangement.spacedBy(6.dp)
		) {
			itemsIndexed(
				items = recordings,
				key = keys,
				contentType = contentType
			) { _, record ->
				RecordingCard(
					music = record.recoding,
					isSelected = record.isSelected,
					isSelectable = isAnySelected,
					onItemClick = { onItemClick(record.recoding) },
					onItemSelect = { onItemSelect(record.recoding) },
					modifier = Modifier
						.fillMaxWidth()
						.animateItem(),
				)
			}
		}

	}
}

@PreviewLightDark
@Composable
private fun RecordingsInteractiveListPreview() = RecorderAppTheme {
	Surface {
		RecordingsInteractiveList(
			recordings = PreviewFakes.FAKE_VOICE_RECORDING_MODELS,
			onItemClick = {},
			onItemSelect = {}
		)
	}
}