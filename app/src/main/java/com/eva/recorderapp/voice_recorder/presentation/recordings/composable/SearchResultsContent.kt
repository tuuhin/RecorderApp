package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.presentation.composables.ListLoadingAnimation
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import com.eva.recorderapp.voice_recorder.presentation.util.RecordedVoiceModelsList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun SearchResultsContent(
	searchResults: RecordedVoiceModelsList,
	onItemSelect: (RecordedVoiceModel) -> Unit,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(),
) {

	val isLocalInspectionMode = LocalInspectionMode.current

	val keys: ((Int, RecordedVoiceModel) -> Any)? = remember {
		if (isLocalInspectionMode) null
		else { _, recording -> recording.id }
	}

	val contentType: ((Int, RecordedVoiceModel) -> Any?) = remember {
		{ _, _ -> RecordedVoiceModel::class.simpleName }
	}

	ListLoadingAnimation(
		isLoaded = true,
		items = searchResults,
		contentPadding = contentPadding,
		modifier = modifier,
		onDataReady = {
			LazyColumn(
				contentPadding = contentPadding,
				verticalArrangement = Arrangement.spacedBy(8.dp)
			) {
				itemsIndexed(
					items = searchResults,
					key = keys,
					contentType = contentType
				) { _, record ->
					RecordingCard(
						music = record,
						isSelected = false,
						isSelectable = false,
						onItemClick = { onItemSelect(record) },
						onItemSelect = { },
						modifier = Modifier
							.fillMaxWidth()
							.animateItem(),
					)
				}
			}
		},
		onNoItems = {
			Box(
				modifier = Modifier.fillMaxSize(),
				contentAlignment = Alignment.Center
			) {
				Text(
					text = stringResource(R.string.no_search_results),
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.secondary
				)
			}
		},
	)
}

private class SearchResultsPreviewParams :
	CollectionPreviewParameterProvider<RecordedVoiceModelsList>(
		listOf(
			PreviewFakes.FAKE_VOICE_RECORDINGS_EMPTY.map { it.recoding }.toImmutableList(),
			PreviewFakes.FAKE_VOICE_RECORDINGS_SELECTED.map { it.recoding }
				.toImmutableList()
		)
	)

@PreviewLightDark
@Composable
private fun SearchResultsContentPreview(
	@PreviewParameter(SearchResultsPreviewParams::class)
	searchResults: RecordedVoiceModelsList,
) = RecorderAppTheme {
	Surface {
		SearchResultsContent(
			searchResults = searchResults,
			onItemSelect = {},
			contentPadding = PaddingValues(
				horizontal = dimensionResource(R.dimen.sc_padding),
				vertical = dimensionResource(R.dimen.sc_padding_secondary)
			),
			modifier = Modifier.fillMaxSize(),
		)
	}
}