package com.eva.feature_categories.composable

import androidx.compose.animation.ExperimentalSharedTransitionApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.feature_categories.CategoriesPreviewFakes
import com.eva.feature_categories.mapper.colorResource
import com.eva.feature_categories.mapper.painter
import com.eva.ui.R
import com.eva.ui.animation.SharedElementTransitionKeys
import com.eva.ui.animation.sharedBoundsWrapper
import com.eva.ui.theme.CustomShapes
import com.eva.ui.theme.RecorderAppTheme
import com.eva.ui.theme.RoundedPolygonShape

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ExtendedCategoryCard(
	category: RecordingCategoryModel,
	onEdit: () -> Unit,
	onDelete: () -> Unit,
	modifier: Modifier = Modifier,
	shape: Shape = MaterialTheme.shapes.large,
	colors: CardColors = CardDefaults.elevatedCardColors(),
) {
	var showDropDown by remember { mutableStateOf(false) }
	var showDeleteDialog by remember { mutableStateOf(false) }

	val iconBackground = category.categoryColor.colorResource
		?: MaterialTheme.colorScheme.surfaceVariant


	DeleteCategoryDialog(
		showDialog = showDeleteDialog,
		onDeleteAfterWarn = onDelete,
		onCancel = { showDeleteDialog = false },
	)

	ElevatedCard(
		colors = colors,
		shape = shape,
		elevation = CardDefaults.cardElevation(
			pressedElevation = 4.dp,
			defaultElevation = 0.dp
		),
		modifier = modifier
			.semantics { role = Role.RadioButton }
			.sharedBoundsWrapper(
				key = SharedElementTransitionKeys.categoryCardSharedBoundsTransition(category.id)
			),
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(6.dp),
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier
				.fillMaxWidth()
				.padding(all = dimensionResource(id = R.dimen.card_padding)),
		) {
			Box(
				modifier = Modifier
					.size(54.dp)
					.background(
						color = iconBackground,
						shape = RoundedPolygonShape(CustomShapes.ROUNDED_STAR)
					)
					.border(
						width = 2.dp,
						color = contentColorFor(iconBackground),
						shape = RoundedPolygonShape(CustomShapes.ROUNDED_STAR)
					)
			) {
				Icon(
					painter = category.categoryType.painter,
					contentDescription = stringResource(
						R.string.category_icon_category,
						category.name
					),
					tint = contentColorFor(iconBackground),
					modifier = Modifier
						.size(28.dp)
						.align(Alignment.Center),
				)
			}
			Spacer(modifier = Modifier.width(2.dp))
			Text(
				text = category.name,
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.primary,
				modifier = Modifier.weight(1f),
			)
			Box {
				IconButton(onClick = { showDropDown = true }) {
					Icon(
						imageVector = Icons.Default.MoreVert,
						contentDescription = stringResource(R.string.menu_more_option)
					)
				}
				DropdownMenu(
					expanded = showDropDown,
					shape = MaterialTheme.shapes.medium,
					onDismissRequest = { showDropDown = false },
				) {
					DropdownMenuItem(
						text = { Text(text = stringResource(R.string.menu_option_edit)) },
						onClick = {
							onEdit()
							showDropDown = false
						},
					)
					DropdownMenuItem(
						text = { Text(text = stringResource(R.string.menu_option_delete)) },
						onClick = {
							showDropDown = false
							showDeleteDialog = true
						},
					)
				}
			}
		}
	}
}


@PreviewLightDark
@Composable
private fun ExtendedCategoryCardPreview() = RecorderAppTheme {
	ExtendedCategoryCard(
		category = CategoriesPreviewFakes.FAKE_CATEGORY_WITH_COLOR_AND_TYPE,
		onDelete = {},
		onEdit = {}
	)
}