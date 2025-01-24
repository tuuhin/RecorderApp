package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.presentation.util.SharedElementTransitionKeys
import com.eva.recorderapp.voice_recorder.presentation.util.sharedBoundsWrapper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun RecorderTopBar(
	showActions: Boolean,
	modifier: Modifier = Modifier,
	onNavigateToBin: () -> Unit = {},
	onNavigateToSettings: () -> Unit = {},
	onNavigateToRecordings: () -> Unit = {},
	navigation: @Composable () -> Unit = {},
	onAddBookMark: () -> Unit = {},
	colors: TopAppBarColors = TopAppBarDefaults
		.topAppBarColors(actionIconContentColor = MaterialTheme.colorScheme.primary),
) {
	var showDropDown by remember { mutableStateOf(false) }

	TopAppBar(
		title = { Text(text = stringResource(id = R.string.recorder_top_bar_title)) },
		navigationIcon = navigation,
		actions = {
			AnimatedContent(
				targetState = showActions,
				label = "Is normal action visible",
				transitionSpec = { bookMarksButtonAnimation() },
				contentAlignment = Alignment.BottomCenter,
				modifier = Modifier.widthIn(min = 60.dp)
			) { isNormal ->
				if (isNormal) {
					TextButton(
						onClick = onNavigateToRecordings,
						modifier = Modifier.sharedBoundsWrapper(key = SharedElementTransitionKeys.RECORDINGS_LIST_SHARED_BOUNDS)
					) {
						Text(
							text = stringResource(id = R.string.show_recordings_list),
							fontWeight = FontWeight.SemiBold
						)
					}
				} else {
					TooltipBox(
						positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
						tooltip = {
							RichTooltip {
								Text(text = stringResource(R.string.bookmark_tooltip_text))
							}
						},
						state = rememberTooltipState()
					) {
						TextButton(onClick = onAddBookMark) {
							Text(
								text = stringResource(id = R.string.add_recording_bookmark),
								fontWeight = FontWeight.SemiBold
							)
						}
					}
				}
			}
			AnimatedVisibility(
				visible = showActions,
				enter = slideInHorizontally() + fadeIn(),
				exit = slideOutHorizontally() + fadeOut()
			) {
				Box {
					IconButton(
						onClick = { showDropDown = !showDropDown },
					) {
						Icon(
							imageVector = Icons.Default.MoreVert,
							contentDescription = stringResource(id = R.string.menu_more_option)
						)
					}
					DropdownMenu(
						expanded = showDropDown,
						onDismissRequest = { showDropDown = false },
						shape = MaterialTheme.shapes.medium
					) {
						DropdownMenuItem(
							text = { Text(text = stringResource(id = R.string.menu_option_recycle_bin)) },
							onClick = {
								onNavigateToBin()
								showDropDown = false
							},
							leadingIcon = {
								Icon(
									painter = painterResource(id = R.drawable.ic_delete),
									contentDescription = stringResource(id = R.string.menu_option_recycle_bin)
								)
							},
						)
						DropdownMenuItem(
							text = { Text(text = stringResource(id = R.string.menu_option_settings)) },
							onClick = {
								onNavigateToSettings()
								showDropDown = false
							},
							leadingIcon = {
								Icon(
									painter = painterResource(id = R.drawable.ic_settings),
									contentDescription = stringResource(id = R.string.menu_option_settings)
								)
							},
						)
					}
				}
			}
		},
		colors = colors,
		modifier = modifier,
	)
}

private fun bookMarksButtonAnimation(): ContentTransform {

	val slideAnimation = spring<IntOffset>(
		dampingRatio = Spring.DampingRatioLowBouncy,
		stiffness = Spring.StiffnessLow
	)

	val fadeAnimation = tween<Float>(durationMillis = 400, easing = FastOutSlowInEasing)

	return slideInHorizontally(slideAnimation) + scaleIn(fadeAnimation) togetherWith
			slideOutHorizontally(slideAnimation) + fadeOut(fadeAnimation)
}

private class BooleanPreviewParams :
	CollectionPreviewParameterProvider<Boolean>(listOf(true, false))

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun RecorderTopBarPreview(
	@PreviewParameter(BooleanPreviewParams::class)
	showActions: Boolean,
) = RecorderAppTheme {
	RecorderTopBar(
		showActions = showActions,
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = stringResource(R.string.back_arrow)
			)
		},
	)
}