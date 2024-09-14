package com.eva.recorderapp.voice_recorder.presentation.categories.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.presentation.composables.ListLoadingAnimation
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.collections.immutable.ImmutableList

@Composable
fun RecordingCategoriesList(
	isLoaded: Boolean,
	onItemClick: (RecordingCategoryModel) -> Unit,
	categories: ImmutableList<RecordingCategoryModel>,
	modifier: Modifier = Modifier,
	selectedCategory: RecordingCategoryModel? = null,
	contentPadding: PaddingValues = PaddingValues(),
) {
	val isLocalInspectionMode = LocalInspectionMode.current

	val keys: ((Int, RecordingCategoryModel) -> Any)? = remember {
		if (isLocalInspectionMode) null
		else { _, category -> category.id }
	}

	val contentType: ((Int, RecordingCategoryModel) -> Any?) = remember {
		{ _, _ -> RecordingCategoryModel::class.simpleName }
	}

	ListLoadingAnimation(
		isLoaded = isLoaded,
		items = categories,
		contentPadding = contentPadding,
		modifier = modifier,
		onDataReady = {
			LazyVerticalGrid(
				columns = GridCells.Fixed(3),
				modifier = Modifier.fillMaxSize(),
				verticalArrangement = Arrangement.spacedBy(12.dp),
				horizontalArrangement = Arrangement.spacedBy(12.dp)
			) {
				itemsIndexed(
					items = categories,
					key = keys,
					contentType = contentType
				) { _, category ->
					RecordingsCategoryCard(
						category = category,
						isSelected = category == selectedCategory,
						onItemClick = { onItemClick(category) },
						modifier = Modifier.animateItem(),
					)
				}
			}
		},
		onNoItems = {
			Column(
				modifier = Modifier.fillMaxSize(),
				verticalArrangement = Arrangement.Center,
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Image(
					painter = painterResource(id = R.drawable.ic_category),
					contentDescription = stringResource(R.string.no_categories_found),
					colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.secondary)
				)
				Spacer(modifier = Modifier.height(20.dp))
				Text(
					text = stringResource(R.string.no_categories_found),
					style = MaterialTheme.typography.titleMedium,
					color = MaterialTheme.colorScheme.tertiary
				)
			}
		}
	)
}

@PreviewLightDark
@Composable
private fun RecordingsCategoriesListPreview() = RecorderAppTheme {
	Surface {
		RecordingCategoriesList(
			isLoaded = true,
			categories = PreviewFakes.FAKE_CATEGORIES_WITH_ALL_OPTION,
			onItemClick = {}
		)
	}
}