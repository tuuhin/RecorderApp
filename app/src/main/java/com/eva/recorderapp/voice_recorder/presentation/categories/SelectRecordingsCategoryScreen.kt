package com.eva.recorderapp.voice_recorder.presentation.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.presentation.categories.composable.RecordingCategoriesList
import com.eva.recorderapp.voice_recorder.presentation.categories.utils.RecordingCategoryEvent
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSnackBarProvider
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectRecordingsCategoryScreen(
	isLoaded: Boolean,
	categories: ImmutableList<RecordingCategoryModel>,
	onEvent: (RecordingCategoryEvent) -> Unit,
	modifier: Modifier = Modifier,
	selectedCategory: RecordingCategoryModel? = null,
	navigation: @Composable () -> Unit = {},
) {
	val snackBarProvider = LocalSnackBarProvider.current
	val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

	Scaffold(
		topBar = {
			MediumTopAppBar(
				title = { Text(text = stringResource(R.string.select_category_screen)) },
				navigationIcon = navigation,
				actions = {
					AnimatedVisibility(
						visible = selectedCategory != null,
						enter = slideInVertically() + fadeIn(),
						exit = slideOutVertically() + fadeOut()
					) {
						TextButton(
							onClick = { onEvent(RecordingCategoryEvent.OnSetRecordingCategory) }
						) {
							Text(text = stringResource(R.string.set_recording_category_action))
						}
					}
				},
			)
		},
		snackbarHost = { SnackbarHost(hostState = snackBarProvider) },
		modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
	) { scPadding ->
		RecordingCategoriesList(
			isLoaded = isLoaded,
			selectedCategory = selectedCategory,
			categories = categories,
			onItemClick = { onEvent(RecordingCategoryEvent.SelectCategory(it)) },
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