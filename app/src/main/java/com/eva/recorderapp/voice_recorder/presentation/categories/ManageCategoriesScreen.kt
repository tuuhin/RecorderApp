package com.eva.recorderapp.voice_recorder.presentation.categories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.presentation.categories.composable.CategoriesBottomBar
import com.eva.recorderapp.voice_recorder.presentation.categories.composable.CategoriesTopBar
import com.eva.recorderapp.voice_recorder.presentation.categories.composable.SelectableCategoriesList
import com.eva.recorderapp.voice_recorder.presentation.categories.utils.CategoriesScreenEvent
import com.eva.recorderapp.voice_recorder.presentation.categories.utils.SelectableCategory
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSnackBarProvider
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import com.eva.recorderapp.voice_recorder.presentation.util.SelectableCategoryImmutableList
import kotlinx.collections.immutable.persistentListOf


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
	isLoaded: Boolean,
	categories: SelectableCategoryImmutableList,
	onScreenEvent: (CategoriesScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
	onNavigateToCreateCategory: () -> Unit = {},
	onNavigateToEditCategory: (RecordingCategoryModel) -> Unit = {},
) {
	val snackBarProvider = LocalSnackBarProvider.current
	val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

	val isAnySelected by remember(categories) {
		derivedStateOf { categories.any(SelectableCategory::isSelected) }
	}

	val selectedCount by remember(categories) {
		derivedStateOf {
			categories.count(SelectableCategory::isSelected)
		}
	}

	val showRenameOption by remember(selectedCount) {
		derivedStateOf { selectedCount == 1 }
	}

	BackHandler(
		enabled = isAnySelected,
		onBack = { onScreenEvent(CategoriesScreenEvent.OnUnSelectAll) },
	)

	Scaffold(
		topBar = {
			CategoriesTopBar(
				isCategorySelected = isAnySelected,
				selectedCount = selectedCount,
				onCreate = onNavigateToCreateCategory,
				onSelectAll = { onScreenEvent(CategoriesScreenEvent.OnSelectAll) },
				onUnSelectAll = { onScreenEvent(CategoriesScreenEvent.OnUnSelectAll) },
				navigation = navigation,
				scrollBehavior = scrollBehavior
			)
		},
		bottomBar = {
			CategoriesBottomBar(
				isVisible = isAnySelected,
				showRename = showRenameOption,
				onRename = {
					categories.firstOrNull { it.isSelected }?.let { category ->
						onNavigateToEditCategory(category.category)
					}
				},
				onDelete = { onScreenEvent(CategoriesScreenEvent.OnDeleteSelected) },
			)
		},
		snackbarHost = { SnackbarHost(hostState = snackBarProvider) },
		modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
	) { scPadding ->
		SelectableCategoriesList(
			isLoaded = isLoaded,
			categories = categories,
			onItemClick = { onScreenEvent(CategoriesScreenEvent.OnToggleSelection(it)) },
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
	CollectionPreviewParameterProvider<SelectableCategoryImmutableList>(
		listOf(
			persistentListOf(),
			PreviewFakes.FAKE_RECORDING_CATEGORIES,
			PreviewFakes.FAKE_RECORDINGS_CATEGORIES_FEW_SELECTED,
		)
	)

@PreviewLightDark
@Composable
private fun ManageCategoriesScreenPreview(
	@PreviewParameter(RecordingsCategoriesPreviewParams::class)
	categories: SelectableCategoryImmutableList,
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