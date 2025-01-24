package com.eva.recorderapp.voice_recorder.presentation.categories.create_category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.ui.theme.RoundedPolygonShape
import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryType
import com.eva.recorderapp.voice_recorder.presentation.util.imageVector
import com.eva.recorderapp.voice_recorder.presentation.util.label

@Composable
fun CategoryTypePicker(
	selectedCategory: CategoryType = CategoryType.CATEGORY_NONE,
	onSelectionChange: (CategoryType) -> Unit = {},
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(10.dp),
) {
	val polygon = remember {
		RoundedPolygon.star(
			numVerticesPerRadius = 8,
			innerRadius = .4f,
			radius = 0.8f,
			rounding = CornerRounding(radius = 0.2f)
		)
	}

	val allowedType = remember {
		CategoryType.entries.filter { it != CategoryType.CATEGORY_NONE }
	}

	LazyVerticalGrid(
		columns = GridCells.Fixed(count = 4),
		horizontalArrangement = Arrangement.spacedBy(12.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
		userScrollEnabled = false,
		contentPadding = contentPadding,
		modifier = modifier
	) {
		itemsIndexed(items = allowedType) { _, type ->

			val shape = remember(polygon) { RoundedPolygonShape(polygon = polygon) }

			CategoryTypeSelector(
				categoryType = type,
				selected = type == selectedCategory,
				onSelectionChange = onSelectionChange,
				shape = shape
			)
		}
	}
}

@Composable
private fun CategoryTypeSelector(
	categoryType: CategoryType,
	onSelectionChange: (CategoryType) -> Unit,
	selected: Boolean,
	shape: Shape = MaterialTheme.shapes.small,
	modifier: Modifier = Modifier,
	containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
	contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
	val interactionSource = remember { MutableInteractionSource() }

	val borderModifier = if (selected) Modifier.border(
		width = 3.dp,
		shape = shape,
		color = MaterialTheme.colorScheme.onSurfaceVariant,
	) else Modifier


	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(4.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Box(
			modifier = Modifier
				.aspectRatio(1f)
				.then(borderModifier)
				.clickable(role = Role.Button) { onSelectionChange(categoryType) }
				.background(color = containerColor, shape = shape)
				.indication(interactionSource = interactionSource, indication = null),
			contentAlignment = Alignment.Center,
		) {
			Icon(
				painter = categoryType.imageVector,
				contentDescription = categoryType.label,
				modifier = Modifier.size(32.dp),
				tint = contentColor
			)
		}
		Text(
			text = categoryType.label,
			style = MaterialTheme.typography.labelMedium,
			color = contentColor
		)
	}

}

@PreviewLightDark
@Composable
private fun CategoryTypePickerPreview() = RecorderAppTheme {
	Surface {
		CategoryTypePicker(
			selectedCategory = CategoryType.CATEGORY_STUDY,
			onSelectionChange = {},
		)
	}
}