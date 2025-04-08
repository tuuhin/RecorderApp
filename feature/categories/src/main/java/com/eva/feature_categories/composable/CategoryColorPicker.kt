package com.eva.feature_categories.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.categories.domain.models.CategoryColor
import com.eva.feature_categories.mapper.colorResource
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun CategoryColorPicker(
	onColorChange: (CategoryColor) -> Unit,
	modifier: Modifier = Modifier,
	selectedColor: CategoryColor = CategoryColor.COLOR_UNKNOWN,
	columns: Int = 8,
	contentPadding: PaddingValues = PaddingValues(10.dp),
	borderColor: Color = MaterialTheme.colorScheme.primary,
) {

	val colorItems = remember {
		CategoryColor.entries.filter { it != CategoryColor.COLOR_UNKNOWN }
	}


	LazyVerticalGrid(
		columns = GridCells.Fixed(count = columns),
		horizontalArrangement = Arrangement.spacedBy(12.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
		userScrollEnabled = false,
		contentPadding = contentPadding,
		modifier = modifier
	) {
		itemsIndexed(colorItems) { _, category ->

			val categoryColor = category.colorResource
				?: MaterialTheme.colorScheme.surfaceContainer

			Spacer(
				modifier = Modifier
					.aspectRatio(1f)
					.graphicsLayer {
						clip = true
						shape = CircleShape
						shadowElevation = 1.dp.toPx()
					}
					.clickable(role = Role.Button) { onColorChange(category) }
					.drawBehind {
						drawCircle(color = categoryColor)
						if (category == selectedColor) {
							drawCircle(color = borderColor, style = Stroke(width = 2.dp.toPx()))
						}
					},
			)
		}
	}
}


@PreviewLightDark
@Composable
private fun CategoryColorPickerPreview() = RecorderAppTheme {
	CategoryColorPicker(onColorChange = {}, selectedColor = CategoryColor.COLOR_RED)
}