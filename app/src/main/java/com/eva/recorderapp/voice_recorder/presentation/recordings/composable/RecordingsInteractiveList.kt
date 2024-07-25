package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.models.TrashRecordingModel
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SelectableRecordings
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.state.SelectableTrashRecordings
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.collections.immutable.ImmutableList

@Composable
fun RecordingsInteractiveList(
	isRecordingsLoaded: Boolean,
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

	RecordingsListInformationStateCrossfade(
		isBin = false,
		isRecordingsLoaded = isRecordingsLoaded,
		recordings = recordings,
		contentPadding = contentPadding,
		modifier = modifier,
		onData = {
			LazyColumn(
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
		},
	)
}

@Composable
fun RecordingsInteractiveList(
	isRecordingsLoaded: Boolean,
	recordings: ImmutableList<SelectableTrashRecordings>,
	onItemSelect: (TrashRecordingModel) -> Unit,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp)
) {
	val isLocalInspectionMode = LocalInspectionMode.current

	val isAnySelected by remember(recordings) {
		derivedStateOf { recordings.any { it.isSelected } }
	}

	val keys: ((Int, SelectableTrashRecordings) -> Any)? = remember {
		if (isLocalInspectionMode) null
		else { _, device -> device.trashRecording.id }
	}

	val contentType: ((Int, SelectableTrashRecordings) -> Any?) = remember {
		{ _, _ -> RecordedVoiceModel::class.simpleName }
	}

	RecordingsListInformationStateCrossfade(
		isBin = true,
		isRecordingsLoaded = isRecordingsLoaded,
		recordings = recordings,
		contentPadding = contentPadding,
		modifier = modifier,
		onData = {
			LazyColumn(
				contentPadding = contentPadding,
				modifier = Modifier.fillMaxSize(),
				verticalArrangement = Arrangement.spacedBy(6.dp)
			) {
				itemsIndexed(
					items = recordings,
					key = keys,
					contentType = contentType
				) { _, record ->
					TrashRecordingsCard(
						trashRecording = record.trashRecording,
						isSelected = record.isSelected,
						isSelectable = isAnySelected,
						onClick = { onItemSelect(record.trashRecording) },
						modifier = Modifier
							.fillMaxWidth()
							.animateItem(),
					)
				}
			}
		},
	)
}

@PreviewLightDark
@Composable
private fun RecordingsInteractiveListRecordingsPreview() = RecorderAppTheme {
	Surface {
		RecordingsInteractiveList(
			isRecordingsLoaded = true,
			recordings = PreviewFakes.FAKE_VOICE_RECORDING_MODELS,
			onItemClick = {},
			onItemSelect = {}
		)
	}
}

@Preview
@Composable
private fun RecordingsInteractiveListTraeshRecordingsPreivew() = RecorderAppTheme {
	Surface {
		RecordingsInteractiveList(
			isRecordingsLoaded = true,
			recordings = PreviewFakes.FAKE_TRASH_RECORDINGS_MODELS,
			onItemSelect = {},
		)
	}
}