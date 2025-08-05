package com.eva.feature_recordings.recordings.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.feature_categories.mapper.colorResource
import com.eva.feature_categories.mapper.painter
import com.eva.ui.R
import com.eva.ui.theme.DownloadableFonts

@Composable
internal fun RecordingCategoryChipSelectorChip(
	isSelected: Boolean,
	category: RecordingCategoryModel,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	selectedCategoryRecordingsCount: Int = 0,
) {
	val chipBackground = category.categoryColor.colorResource
		?: MaterialTheme.colorScheme.surfaceContainer

	val chipContentColor = contentColorFor(chipBackground)

	FilterChip(
		selected = isSelected,
		onClick = onClick,
		label = { Text(text = category.name) },
		shape = MaterialTheme.shapes.medium,
		leadingIcon = {
			AnimatedVisibility(
				visible = isSelected,
			) {
				Icon(
					painter = category.categoryType.painter,
					contentDescription = stringResource(
						R.string.category_icon_category,
						category.name
					),
					modifier = Modifier.sizeIn(maxHeight = 20.dp, maxWidth = 20.dp),
				)
			}
		},
		trailingIcon = {
			AnimatedVisibility(
				visible = selectedCategoryRecordingsCount > 0 && isSelected,
				enter = slideInHorizontally() + fadeIn(),
				exit = slideOutHorizontally() + fadeOut()
			) {
				Text(
					text = "(${selectedCategoryRecordingsCount})",
					style = MaterialTheme.typography.labelMedium,
					fontFamily = DownloadableFonts.NOVA_MONO_FONT_FAMILY
				)
			}
		},
		colors = FilterChipDefaults.filterChipColors(
			selectedContainerColor = chipBackground,
			selectedLabelColor = chipContentColor,
			selectedLeadingIconColor = chipContentColor,
			containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
			iconColor = chipContentColor
		),
		modifier = modifier.animateContentSize(),
	)
}
