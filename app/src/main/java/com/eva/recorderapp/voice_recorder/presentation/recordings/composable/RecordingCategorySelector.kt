package com.eva.recorderapp.voice_recorder.presentation.recordings.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import com.eva.recorderapp.voice_recorder.presentation.util.colorRes
import com.eva.recorderapp.voice_recorder.presentation.util.imageVector
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

			val chipBackground = category.categoryColor.colorRes
				?: MaterialTheme.colorScheme.surfaceContainer

			val chipContentColor = contentColorFor(chipBackground)

			InputChip(
				selected = selected == category,
				onClick = { onCategorySelect(category) },
				label = { Text(text = category.name) },
				shape = MaterialTheme.shapes.medium,
				leadingIcon = {
					AnimatedVisibility(
						visible = selected == category,
						enter = scaleIn() + fadeIn(),
						exit = scaleOut() + fadeOut()
					) {
						Icon(
							painter = category.categoryType.imageVector,
							contentDescription = stringResource(
								id = R.string.category_icon_category,
								category.name
							),
						)
					}
				},
				trailingIcon = {
					AnimatedVisibility(
						visible = category.hasCount,
						enter = slideInHorizontally() + fadeIn(),
						exit = slideOutHorizontally() + fadeOut()
					) {
						Text(text = "(${category.count})")
					}
				},
				colors = InputChipDefaults.inputChipColors(
					selectedContainerColor = chipBackground,
					selectedLabelColor = chipContentColor,
					trailingIconColor = chipContentColor,
					leadingIconColor = chipContentColor
				),
				modifier = Modifier.animateItem(),
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