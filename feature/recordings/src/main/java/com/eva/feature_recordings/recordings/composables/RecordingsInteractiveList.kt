package com.eva.feature_recordings.recordings.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.feature_recordings.recordings.state.SelectableRecordings
import com.eva.feature_recordings.util.RecordingsPreviewFakes
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.ui.R
import com.eva.ui.composables.ListLoadingAnimation
import com.eva.ui.theme.RecorderAppTheme
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun RecordingsInteractiveList(
	isRecordingsLoaded: Boolean,
	selectedCategory: RecordingCategoryModel,
	categories: ImmutableList<RecordingCategoryModel>,
	recordings: ImmutableList<SelectableRecordings>,
	onItemClick: (RecordedVoiceModel) -> Unit,
	onItemSelect: (RecordedVoiceModel) -> Unit,
	onCategorySelect: (RecordingCategoryModel) -> Unit,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp),
) {
	val isLocalInspectionMode = LocalInspectionMode.current

	val isAnySelected by remember(recordings) {
		derivedStateOf { recordings.any { it.isSelected } }
	}

	val keys: ((Int, SelectableRecordings) -> Any)? = remember {
		if (isLocalInspectionMode) null
		else { _, device -> device.recording.id }
	}

	val contentType: ((Int, SelectableRecordings) -> Any?) = remember {
		{ _, _ -> RecordedVoiceModel::class.simpleName }
	}

	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(4.dp)
	) {
		RecordingsCategorySelector(
			selected = selectedCategory,
			categories = categories,
			onCategorySelect = onCategorySelect,
			contentPadding = PaddingValues(horizontal = dimensionResource(id = R.dimen.sc_padding))
		)
		HorizontalDivider()
		ListLoadingAnimation(
			isLoaded = isRecordingsLoaded,
			items = recordings,
			contentPadding = contentPadding,
			modifier = Modifier.weight(1f),
			onDataReady = { items ->
				LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
					itemsIndexed(
						items = items,
						key = keys,
						contentType = contentType
					) { _, record ->
						RecordingCard(
							music = record.recording,
							isSelected = record.isSelected,
							isSelectable = isAnySelected,
							onItemClick = { onItemClick(record.recording) },
							onItemSelect = { onItemSelect(record.recording) },
							modifier = Modifier
								.fillMaxWidth()
								.animateItem(),
						)
					}
				}
			},
			onNoItems = {
				Column(
					modifier = Modifier.fillMaxSize(),
					verticalArrangement = Arrangement.Center,
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					Image(
						painter = painterResource(id = R.drawable.ic_recorder),
						contentDescription = stringResource(R.string.no_recordings),
						colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.secondary)
					)
					Spacer(modifier = Modifier.height(20.dp))
					Text(
						text = stringResource(id = R.string.no_recordings),
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.tertiary
					)
				}
			},
		)
	}
}

@PreviewLightDark
@Composable
private fun RecordingsInteractiveListRecordingsPreview() = RecorderAppTheme {
	Surface {
		RecordingsInteractiveList(
			isRecordingsLoaded = true,
			selectedCategory = RecordingCategoryModel.ALL_CATEGORY,
			categories = RecordingsPreviewFakes.FAKE_CATEGORIES_WITH_ALL_OPTION,
			recordings = RecordingsPreviewFakes.FAKE_VOICE_RECORDING_MODELS,
			onCategorySelect = {},
			onItemClick = {},
			onItemSelect = {}
		)
	}
}
