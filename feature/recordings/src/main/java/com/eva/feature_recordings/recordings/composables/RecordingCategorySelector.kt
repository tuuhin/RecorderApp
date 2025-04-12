package com.eva.feature_recordings.recordings.composables

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
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.feature_recordings.util.RecordingsPreviewFakes
import com.eva.ui.theme.RecorderAppTheme
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun RecordingsCategorySelector(
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
		{ _, _ -> RecordingCategoryModel::class.simpleName }
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
			categories = RecordingsPreviewFakes.FAKE_CATEGORIES_WITH_ALL_OPTION,
			onCategorySelect = {},
		)
	}
}