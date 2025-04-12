package com.eva.feature_categories.category_picker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.feature_categories.CategoriesPreviewFakes
import com.eva.feature_categories.composable.SelectableCategoriesCardGrid
import com.eva.ui.R
import com.eva.ui.animation.SharedElementTransitionKeys
import com.eva.ui.animation.sharedBoundsWrapper
import com.eva.ui.theme.RecorderAppTheme
import com.eva.ui.utils.LocalSnackBarProvider
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun CategoryPickerScreen(
	isLoaded: Boolean,
	categories: ImmutableList<RecordingCategoryModel>,
	onEvent: (CategoryPickerEvent) -> Unit,
	modifier: Modifier = Modifier,
	selectedCategory: RecordingCategoryModel? = null,
	navigation: @Composable () -> Unit = {},
	onNavigateToCreateNew: () -> Unit = {},
) {
	val snackBarProvider = LocalSnackBarProvider.current
	val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

	Scaffold(
		topBar = {
			MediumTopAppBar(
				title = { Text(text = stringResource(R.string.select_category_screen)) },
				actions = {
					TextButton(
						onClick = onNavigateToCreateNew,
						colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
						modifier = Modifier.sharedBoundsWrapper(key = SharedElementTransitionKeys.categoryCardSharedBoundsTransition())
					) {
						Text(text = stringResource(R.string.add_new_category))
					}
				},
				navigationIcon = navigation,
				scrollBehavior = scrollBehavior,
			)
		},
		floatingActionButton = {
			AnimatedVisibility(
				visible = selectedCategory != null,
				enter = slideInVertically() + fadeIn(),
				exit = slideOutVertically() + fadeOut()
			) {
				ExtendedFloatingActionButton(
					onClick = { onEvent(CategoryPickerEvent.OnSetRecordingCategory) },
					elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
				) {
					Icon(
						imageVector = Icons.Default.Check,
						contentDescription = stringResource(R.string.set_recording_category_action)
					)
					Spacer(modifier = Modifier.width(4.dp))
					Text(text = stringResource(R.string.set_recording_category_action))
				}
			}
		},
		snackbarHost = { SnackbarHost(hostState = snackBarProvider) },
		modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
	) { scPadding ->
		SelectableCategoriesCardGrid(
			isLoaded = isLoaded,
			selectedCategory = selectedCategory,
			categories = categories,
			onItemClick = { onEvent(CategoryPickerEvent.SelectCategory(it)) },
			contentPadding = PaddingValues(
				start = dimensionResource(id = R.dimen.sc_padding),
				end = dimensionResource(id = R.dimen.sc_padding),
				top = dimensionResource(id = R.dimen.sc_padding) + scPadding.calculateTopPadding(),
				bottom = dimensionResource(id = R.dimen.sc_padding) + scPadding.calculateBottomPadding()
			),
			modifier = Modifier.fillMaxSize(),
		)
	}
}

class RecordingCategoryNullAblePreviewParams :
	CollectionPreviewParameterProvider<RecordingCategoryModel?>(
		listOf(
			null,
			CategoriesPreviewFakes.FAKE_CATEGORY_WITH_COLOR_AND_TYPE
		)
	)

@PreviewLightDark
@Composable
private fun CategoryPicketScreenPreview(
	@PreviewParameter(RecordingCategoryNullAblePreviewParams::class)
	selectedCategory: RecordingCategoryModel?,
) = RecorderAppTheme {
	CategoryPickerScreen(
		isLoaded = true,
		categories = CategoriesPreviewFakes.FAKE_CATEGORIES_WITH_ALL_OPTION,
		onEvent = {},
		selectedCategory = selectedCategory,
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = stringResource(R.string.back_arrow)
			)
		},
	)
}