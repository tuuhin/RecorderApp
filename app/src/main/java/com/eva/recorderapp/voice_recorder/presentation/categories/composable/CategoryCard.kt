package com.eva.recorderapp.voice_recorder.presentation.categories.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import com.eva.recorderapp.voice_recorder.presentation.util.colorRes
import com.eva.recorderapp.voice_recorder.presentation.util.imageVector

@Composable
fun CategoryCard(
	category: RecordingCategoryModel,
	onItemClick: () -> Unit,
	modifier: Modifier = Modifier,
	isSelected: Boolean = false,
	borderColor: Color = MaterialTheme.colorScheme.secondary,
) {
	val containerColor = category.categoryColor.colorRes
		?: MaterialTheme.colorScheme.secondaryContainer

	val border = remember(isSelected) {
		if (isSelected) BorderStroke(width = 2.dp, color = borderColor)
		else null
	}

	Card(
		onClick = onItemClick,
		colors = CardDefaults.cardColors(
			containerColor = containerColor,
			contentColor = contentColorFor(containerColor)
		),
		elevation = CardDefaults.elevatedCardElevation(pressedElevation = 4.dp),
		border = border,
		modifier = modifier
			.semantics { role = Role.RadioButton },
	) {
		Column(
			modifier = Modifier
				.padding(12.dp)
				.align(Alignment.CenterHorizontally),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			Icon(
				painter = category.categoryType.imageVector,
				contentDescription = category.name,
				modifier = Modifier
					.sizeIn(minHeight = 24.dp, minWidth = 24.dp)
					.align(Alignment.CenterHorizontally)
			)
			Spacer(modifier = Modifier.height(6.dp))
			Text(
				text = category.name,
				style = MaterialTheme.typography.bodyMedium,
				overflow = TextOverflow.Ellipsis,
				maxLines = 1,
				textAlign = TextAlign.Center,
				modifier = Modifier.align(Alignment.CenterHorizontally)
			)
		}
	}
}

@PreviewLightDark
@Composable
private fun CategoryCardPreview() = RecorderAppTheme {
	CategoryCard(
		category = PreviewFakes.FAKE_CATEGORY_WITH_COLOR_AND_TYPE,
		onItemClick = {},
		isSelected = true,
		modifier = Modifier.padding(4.dp)
	)
}