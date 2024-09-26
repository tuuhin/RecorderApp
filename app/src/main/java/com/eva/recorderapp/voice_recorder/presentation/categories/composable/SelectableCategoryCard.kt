package com.eva.recorderapp.voice_recorder.presentation.categories.composable

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.presentation.categories.utils.SelectableCategory
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import com.eva.recorderapp.voice_recorder.presentation.util.colorRes
import com.eva.recorderapp.voice_recorder.presentation.util.imageVector

@Composable
fun SelectableCategoryCard(
	category: SelectableCategory,
	onItemClick: () -> Unit,
	modifier: Modifier = Modifier,
	isSelected: Boolean = false,
	isSelectable: Boolean = false,
	shape: Shape = MaterialTheme.shapes.large,
	colors: CardColors = CardDefaults.elevatedCardColors(),
) {
	ElevatedCard(
		colors = colors,
		shape = shape,
		elevation = CardDefaults.cardElevation(
			pressedElevation = 4.dp,
			defaultElevation = 0.dp
		),
		onClick = onItemClick,
		modifier = modifier.semantics { role = Role.RadioButton },
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(4.dp),
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier
				.fillMaxWidth()
				.padding(all = dimensionResource(id = R.dimen.card_padding)),
		) {
			Crossfade(
				targetState = isSelectable,
				animationSpec = tween(durationMillis = 400),
				label = "Radio Button Animation",
				modifier = Modifier.padding(8.dp)
			) { showSelectOption ->
				if (showSelectOption)
					RadioButton(
						selected = isSelected,
						onClick = onItemClick,
						colors = RadioButtonDefaults
							.colors(selectedColor = MaterialTheme.colorScheme.secondary),
						modifier = Modifier.size(32.dp)
					)
				else Icon(
					painter = category.category.categoryType.imageVector,
					contentDescription = stringResource(
						R.string.category_icon_category,
						category.category.name
					),
					modifier = Modifier.size(32.dp),
					tint = MaterialTheme.colorScheme.secondary,
				)
			}
			Spacer(modifier = Modifier.width(2.dp))
			Text(
				text = category.category.name,
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.primary,
				modifier = Modifier.weight(1f),
			)
			category.category.categoryColor.colorRes?.let { color ->
				Box(
					modifier = Modifier
						.size(32.dp)
						.background(color = color, shape = CircleShape)
						.border(
							width = 1.5.dp,
							color = MaterialTheme.colorScheme.secondary,
							shape = CircleShape
						)
				)
			}
		}
	}
}


@PreviewLightDark
@Composable
private fun RecordingCategoryCardNotSelectablePreview() = RecorderAppTheme {
	SelectableCategoryCard(
		category = SelectableCategory(category = PreviewFakes.FAKE_CATEGORY_WITH_COLOR_AND_TYPE),
		onItemClick = {},
		isSelectable = false,
	)
}

@PreviewLightDark
@Composable
private fun RecordingCategoryCardSelectablePreview() = RecorderAppTheme {
	SelectableCategoryCard(
		category = SelectableCategory(category = PreviewFakes.FAKE_CATEGORY_WITH_COLOR_AND_TYPE),
		onItemClick = {},
		isSelectable = true,
	)
}