package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.collections.immutable.ImmutableList

@Composable
fun RecordingsCategorySelector(
	selected: RecordingCategoryModel,
	categories: ImmutableList<RecordingCategoryModel>,
	onCategorySelect: (RecordingCategoryModel) -> Unit,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(),
) {
	val isLocalInspectionMode = LocalInspectionMode.current

	val keys: ((Int, RecordingCategoryModel) -> Any)? = remember {
		if (isLocalInspectionMode) null
		else { _, category -> category.id }
	}

	val contentType: ((Int, RecordingCategoryModel) -> Any?) = remember {
		{ _, _ -> RecordedVoiceModel::class.simpleName }
	}

	LazyRow(
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		contentPadding = contentPadding,
		modifier = modifier,
	) {
		itemsIndexed(
			items = categories,
			key = keys,
			contentType = contentType
		) { _, category ->

			RecordingCategoryChipSelectorChip(
				category = category,
				isSelected = selected == category,
				onClick = { onCategorySelect(category) },
				modifier = Modifier.animateItem()
			)
		}
	}
}

@PreviewLightDark
@Composable
private fun RecordingsCategorySelectorPreview() = RecorderAppTheme {
	Surface {
		RecordingsCategorySelector(
			selected = RecordingCategoryModel.ALL_CATEGORY,
			categories = PreviewFakes.FAKE_CATEGORIES_WITH_ALL_OPTION,
			onCategorySelect = {},
		)
	}
}