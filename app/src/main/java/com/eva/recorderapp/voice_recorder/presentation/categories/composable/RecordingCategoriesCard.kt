package com.eva.recorderapp.voice_recorder.presentation.categories.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes

@Composable
fun RecordingsCategoryCard(
	category: RecordingCategoryModel,
	onItemClick: () -> Unit,
	modifier: Modifier = Modifier,
	isSelected: Boolean = false,
	shape: Shape = MaterialTheme.shapes.large,
	colors: CardColors = CardDefaults.elevatedCardColors(),
) {
	Card(
		colors = colors,
		shape = shape,
		elevation = CardDefaults.elevatedCardElevation(pressedElevation = 4.dp),
		onClick = onItemClick,
		modifier = modifier.semantics {
			role = Role.RadioButton
		},
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(4.dp),
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier
				.fillMaxWidth()
				.padding(all = dimensionResource(id = R.dimen.card_padding)),
		) {
			RadioButton(
				selected = isSelected,
				onClick = onItemClick,
				colors = RadioButtonDefaults
					.colors(selectedColor = MaterialTheme.colorScheme.secondary),
			)
			Text(
				text = category.name,
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.primary,
				modifier = Modifier.weight(1f),
			)
		}
	}
}

@PreviewLightDark
@Composable
private fun RecordingCategoryCardPreview() = RecorderAppTheme {
	RecordingsCategoryCard(
		category = PreviewFakes.FAKE_RECORDING_CATEGORY.category,
		onItemClick = {},
	)
}