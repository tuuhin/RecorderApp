package com.eva.recorderapp.voice_recorder.presentation.categories.create_category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun CategoryTypePicker(
	onSelectionChange: (CategoryType) -> Unit,
	modifier: Modifier = Modifier,
	selected: CategoryType = CategoryType.CATEGORY_NONE,
	columns: Int = 4,
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
		columns = GridCells.Fixed(count = columns),
		horizontalArrangement = Arrangement.spacedBy(12.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
		userScrollEnabled = false,
		contentPadding = contentPadding,
		modifier = modifier
	) {
		itemsIndexed(items = allowedType) { _, type ->

			val borderModifier = if (type == selected)
				Modifier.border(
					width = 3.dp,
					color = MaterialTheme.colorScheme.onPrimaryContainer,
					shape = RoundedPolygonShape(polygon)
				)
			else Modifier

			Box(
				modifier = Modifier
					.aspectRatio(1f)
					.then(borderModifier)
					.clickable(role = Role.Button) { onSelectionChange(type) }
					.background(
						color = MaterialTheme.colorScheme.surfaceContainerHigh,
						shape = RoundedPolygonShape(polygon)
					),
				contentAlignment = Alignment.Center,
			) {
				Icon(
					painter = type.imageVector,
					contentDescription = null,
					modifier = Modifier.size(32.dp),
					tint = MaterialTheme.colorScheme.onPrimaryContainer
				)
			}
		}
	}
}

@PreviewLightDark
@Composable
private fun CategoryTypePickerPreview() = RecorderAppTheme {
	Surface {
		CategoryTypePicker(
			selected = CategoryType.CATEGORY_STUDY,
			onSelectionChange = {},
		)
	}
}