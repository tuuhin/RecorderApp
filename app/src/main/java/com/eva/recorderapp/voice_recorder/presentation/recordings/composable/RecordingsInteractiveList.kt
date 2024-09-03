package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.TrashRecordingModel
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

	RecordingsListDataCrossfade(
		isBin = false,
		isRecordingsLoaded = isRecordingsLoaded,
		recordings = recordings,
		contentPadding = contentPadding,
		modifier = modifier,
		onData = {
			LazyColumn(
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

@OptIn(ExperimentalFoundationApi::class)
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

	RecordingsListDataCrossfade(
		isBin = true,
		isRecordingsLoaded = isRecordingsLoaded,
		recordings = recordings,
		contentPadding = contentPadding,
		modifier = modifier,
		onData = {
			LazyColumn(
				modifier = Modifier.fillMaxSize(),
				verticalArrangement = Arrangement.spacedBy(6.dp)
			) {
				stickyHeader {
					Card(
						shape = MaterialTheme.shapes.medium,
						colors = CardDefaults.cardColors(
							containerColor = MaterialTheme.colorScheme.tertiaryContainer,
							contentColor = MaterialTheme.colorScheme.onTertiaryContainer
						)
					) {
						Row(
							modifier = Modifier.padding(dimensionResource(id = R.dimen.card_padding)),
							horizontalArrangement = Arrangement.spacedBy(8.dp),
							verticalAlignment = Alignment.CenterVertically
						) {
							Icon(
								painter = painterResource(id = R.drawable.ic_info),
								contentDescription = stringResource(R.string.extras_info)
							)
							Text(
								text = stringResource(R.string.recording_bin_explain_text),
								style = MaterialTheme.typography.labelLarge,
								color = MaterialTheme.colorScheme.tertiary,
							)
						}
					}
				}
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
private fun RecordingsInteractiveListTrashRecordingsPreview() = RecorderAppTheme {
	Surface {
		RecordingsInteractiveList(
			isRecordingsLoaded = true,
			recordings = PreviewFakes.FAKE_TRASH_RECORDINGS_MODELS,
			onItemSelect = {},
			contentPadding = PaddingValues(10.dp)
		)
	}
}