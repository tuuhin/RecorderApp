package com.eva.recorderapp.voice_recorder.presentation.categories.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.presentation.categories.utils.SelectableCategory
import com.eva.recorderapp.voice_recorder.presentation.composables.ListLoadingAnimation
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SelectableCategoriesList(
	isLoaded: Boolean,
	onItemClick: (RecordingCategoryModel) -> Unit,
	categories: ImmutableList<SelectableCategory>,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(),
) {
	val isLocalInspectionMode = LocalInspectionMode.current

	val keys: ((Int, SelectableCategory) -> Any)? = remember {
		if (isLocalInspectionMode) null
		else { _, device -> device.category.id }
	}

	val contentType: ((Int, SelectableCategory) -> Any?) = remember {
		{ _, _ -> RecordedVoiceModel::class.simpleName }
	}

	ListLoadingAnimation(
		isLoaded = isLoaded,
		items = categories,
		contentPadding = contentPadding,
		modifier = modifier,
		onDataReady = {
			LazyColumn(
				modifier = Modifier.fillMaxSize(),
				verticalArrangement = Arrangement.spacedBy(6.dp)
			) {
				itemsIndexed(
					items = categories,
					key = keys,
					contentType = contentType
				) { _, category ->
					SelectableCategoryCard(
						category = category,
						isSelected = category.isSelected,
						onItemClick = { onItemClick(category.category) },
						modifier = Modifier
							.fillMaxWidth()
							.animateItem(),
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