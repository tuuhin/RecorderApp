package com.eva.feature_categories.manage_category

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.feature_categories.CategoriesPreviewFakes
import com.eva.feature_categories.composable.CategoriesCardList
import com.eva.ui.R
import com.eva.ui.animation.SharedElementTransitionKeys
import com.eva.ui.animation.sharedBoundsWrapper
import com.eva.ui.theme.RecorderAppTheme
import com.eva.ui.utils.LocalSnackBarProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf


@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun ManageCategoriesScreen(
	isLoaded: Boolean,
	categories: ImmutableList<RecordingCategoryModel>,
	onScreenEvent: (ManageCategoriesEvent) -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
	onNavigateToCreateCategory: () -> Unit = {},
	onNavigateToEditCategory: (RecordingCategoryModel) -> Unit = {},
) {
	val snackBarProvider = LocalSnackBarProvider.current
	val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

	Scaffold(
		topBar = {
			MediumTopAppBar(
				title = { Text(text = stringResource(id = R.string.manage_categories_top_bar_title)) },
				actions = {
					TextButton(
						onClick = onNavigateToCreateCategory,
						colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
						modifier = Modifier.sharedBoundsWrapper(key = SharedElementTransitionKeys.categoryCardSharedBoundsTransition())
					) {
						Text(text = stringResource(R.string.menu_option_create))
					}
				},
				navigationIcon = navigation,
				scrollBehavior = scrollBehavior,
			)
		},
		snackbarHost = { SnackbarHost(hostState = snackBarProvider) },
		modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
	) { scPadding ->
		CategoriesCardList(
			isLoaded = isLoaded,
			categories = categories,
			onDelete = { onScreenEvent(ManageCategoriesEvent.OnDeleteCategory(it)) },
			onEdit = { onNavigateToEditCategory(it) },
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

class RecordingsCategoriesPreviewParams :
	CollectionPreviewParameterProvider<List<RecordingCategoryModel>>(
		listOf(
			persistentListOf(),
			CategoriesPreviewFakes.FAKE_RECORDING_CATEGORIES,
		)
	)

@PreviewLightDark
@Composable
private fun ManageCategoriesScreenPreview(
	@PreviewParameter(RecordingsCategoriesPreviewParams::class)
	categories: ImmutableList<RecordingCategoryModel>,
) = RecorderAppTheme {
	ManageCategoriesScreen(
		isLoaded = true,
		categories = categories,
		onScreenEvent = {},
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = stringResource(R.string.back_arrow)
			)
		},
	)
}