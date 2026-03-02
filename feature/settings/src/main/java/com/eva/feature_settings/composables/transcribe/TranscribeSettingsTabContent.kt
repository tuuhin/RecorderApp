package com.eva.feature_settings.composables.transcribe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eva.datastore.domain.models.TranscriptionSettings
import com.eva.feature_settings.composables.SettingsItemTitle
import com.eva.feature_settings.utils.TranscriptionSettingsEvents
import com.eva.transcribe.domain.models.STTModel
import com.eva.ui.R
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TranscribeSettingsTabContent(
	sttModels: ImmutableList<STTModel>,
	modifier: Modifier = Modifier,
	isTranscriptionActive: Boolean = false,
	selectedSTTModel: STTModel? = null,
	onToggleTranscriptionActive: (Boolean) -> Unit = {},
	onSelectSTTModel: (STTModel) -> Unit = {},
	onUnSelectSTTModel: () -> Unit = {},
	onDownloadSTTModel: (STTModel) -> Unit = {},
	onDeleteSTTModel: (STTModel) -> Unit = {},
	onSelectInvalidModel: (STTModel) -> Unit = {},
	contentPadding: PaddingValues = PaddingValues(12.dp)
) {

	val isInspectionMode = LocalInspectionMode.current

	val listKey: ((Int, STTModel) -> Any)? =
		remember { if (isInspectionMode) null else { _, content -> content.modelId } }

	val listContentType: (Int, STTModel) -> Any =
		remember {
			{ _, content ->
				if (isInspectionMode) 0
				else content.javaClass.simpleName
			}
		}


	LazyColumn(
		modifier = modifier,
		contentPadding = contentPadding,
		verticalArrangement = Arrangement.spacedBy(6.dp)
	) {
		item {
			SettingsItemTitle(
				title = stringResource(R.string.recordings_settings_transcriptions_title),
				text = stringResource(R.string.recordings_settings_transcriptions_desc),
				modifier = Modifier.padding(vertical = 6.dp)
			)
		}
		item {
			ListItem(
				headlineContent = { Text(text = stringResource(R.string.option_enable)) },
				trailingContent = {
					Switch(
						checked = isTranscriptionActive,
						onCheckedChange = onToggleTranscriptionActive,
					)
				},
				colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
				modifier = Modifier.clip(MaterialTheme.shapes.extraLarge)
			)
		}
		item {
			SettingsItemTitle(
				title = stringResource(R.string.stt_models_list_title),
				titleStyle = MaterialTheme.typography.titleMedium,
				titleColor = MaterialTheme.colorScheme.onSurface,
				modifier = Modifier.padding(vertical = 6.dp)
			)
		}
		itemsIndexed(
			items = sttModels,
			key = listKey,
			contentType = listContentType
		) { _, model ->
			STTModelOptionCard(
				sttModel = model,
				isSelected = model.modelId == selectedSTTModel?.modelId,
				onDownload = { onDownloadSTTModel(model) },
				onDelete = { onDeleteSTTModel(model) },
				onSelect = { onSelectSTTModel(model) },
				onUnSelect = onUnSelectSTTModel,
				onSelectInvalid = { onSelectInvalidModel(model) },
				enabled = isTranscriptionActive,
				modifier = Modifier
					.fillMaxWidth()
					.animateItem()
			)
		}
	}
}

@Composable
internal fun TranscribeSettingsTabContent(
	sttModels: ImmutableList<STTModel>,
	modifier: Modifier = Modifier,
	settings: TranscriptionSettings = TranscriptionSettings(),
	onEvent: (TranscriptionSettingsEvents) -> Unit,
	contentPadding: PaddingValues = PaddingValues.Zero,
) {
	val selectedSTTModel by remember(settings.modelId) {
		derivedStateOf { sttModels.find { it.modelId == settings.modelId } }
	}

	TranscribeSettingsTabContent(
		sttModels = sttModels,
		contentPadding = contentPadding,
		isTranscriptionActive = settings.isEnabled,
		selectedSTTModel = selectedSTTModel,
		onUnSelectSTTModel = { onEvent(TranscriptionSettingsEvents.OnSelectModel()) },
		onToggleTranscriptionActive = { onEvent(TranscriptionSettingsEvents.OnToggleTranscriptions) },
		onDownloadSTTModel = { model ->
			onEvent(TranscriptionSettingsEvents.OnStartModelDownload(model))
		},
		onDeleteSTTModel = { model ->
			onEvent(TranscriptionSettingsEvents.OnDeleteDownloadedModel(model))
		},
		onSelectSTTModel = { model ->
			onEvent(TranscriptionSettingsEvents.OnSelectModel(model))
		},
		onSelectInvalidModel = { model ->
			onEvent(TranscriptionSettingsEvents.OnSelectInvalidModel(model))
		},
		modifier = modifier.fillMaxSize(),
	)
}