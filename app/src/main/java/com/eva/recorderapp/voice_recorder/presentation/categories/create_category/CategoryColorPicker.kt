package com.eva.recorderapp.voice_recorder.presentation.categories.create_category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryColor
import com.eva.recorderapp.voice_recorder.presentation.util.colorRes

@Composable
fun CategoryColorPicker(
	onColorChange: (CategoryColor) -> Unit,
	modifier: Modifier = Modifier,
	selectedColor: CategoryColor = CategoryColor.COLOR_UNKNOWN,
	columns: Int = 8,
	contentPadding: PaddingValues = PaddingValues(10.dp),
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

			val selectedModifier = if (category == selectedColor) {
				Modifier.border(
					width = 2.dp,
					color = MaterialTheme.colorScheme.primary,
					shape = CircleShape
				)
			} else Modifier

			Box(
				modifier = Modifier
					.aspectRatio(1f)
					.graphicsLayer {
						clip = true
						shape = CircleShape
						shadowElevation = 1.dp.toPx()
					}
					.clickable(role = Role.Button) { onColorChange(category) }
					.background(
						color = category.colorRes ?: MaterialTheme.colorScheme.surfaceContainer
					)
					.then(selectedModifier),
			)
		}
	}
}


@PreviewLightDark
@Composable
private fun CategoryColorPickerPreview() = RecorderAppTheme {
	CategoryColorPicker(onColorChange = {}, selectedColor = CategoryColor.COLOR_RED)
}