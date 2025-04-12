package com.eva.feature_player.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.feature_player.state.ContentLoadState
import com.eva.feature_player.util.PlayerPreviewFakes
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.ui.R
import com.eva.ui.animation.SharedElementTransitionKeys
import com.eva.ui.animation.sharedBoundsWrapper
import com.eva.ui.theme.RecorderAppTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun AudioPlayerScreenTopBar(
	state: ContentLoadState,
	modifier: Modifier = Modifier,
	onEdit: () -> Unit,
	navigation: @Composable () -> Unit = {},
	onRenameOption: (AudioFileModel) -> Unit = {},
	onShareOption: () -> Unit = {},
	onDetailsOptions: () -> Unit = {},
	onToggleFavourite: (AudioFileModel) -> Unit = {},
	scrollBehavior: TopAppBarScrollBehavior? = null,
	colors: TopAppBarColors = TopAppBarDefaults
		.topAppBarColors(actionIconContentColor = MaterialTheme.colorScheme.primary),
) {
	var showDropDown by remember { mutableStateOf(false) }

	val isActionEnabled by remember(state) {
		derivedStateOf {
			state is ContentLoadState.Content
		}
	}
	state.OnContentOrOther(
		content = { model ->
			TopAppBar(
				title = {
					Text(
						text = model.displayName,
						overflow = TextOverflow.Ellipsis,
						maxLines = 1,
						modifier = Modifier.sharedBoundsWrapper(
							key = SharedElementTransitionKeys.recordSharedEntryTitle(model.id)
						)
					)
				},
				actions = {
					TooltipBox(
						positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
						tooltip = {
							PlainTooltip {
								Text(
									text = if (model.isFavourite)
										stringResource(R.string.menu_option_favourite)
									else stringResource(R.string.menu_option_no_favourite)
								)
							}
						},
						state = rememberTooltipState()
					) {
						IconButton(onClick = { onToggleFavourite(model) }) {
							AnimatedContent(
								targetState = model.isFavourite,
								label = "Is Audio Favourite",
								transitionSpec = { favouriteAudioAnimation() },
							) { isFavourite ->
								if (isFavourite)
									Icon(
										painter = painterResource(R.drawable.ic_star_filled),
										contentDescription = stringResource(R.string.menu_option_favourite),
										modifier = Modifier.size(20.dp)
									)
								else Icon(
									painter = painterResource(R.drawable.ic_star_outlined),
									contentDescription = stringResource(R.string.menu_option_no_favourite),
									modifier = Modifier.size(20.dp)
								)
							}
						}
					}


					TooltipBox(
						positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
						tooltip = {
							PlainTooltip {
								Text(text = stringResource(id = R.string.player_action_edit))
							}
						},
						state = rememberTooltipState(),
					) {
						IconButton(onClick = onEdit) {
							Icon(
								painter = painterResource(id = R.drawable.ic_edit),
								contentDescription = stringResource(id = R.string.player_action_edit)
							)
						}
					}
					TooltipBox(
						positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
						tooltip = {
							PlainTooltip {
								Text(text = stringResource(id = R.string.menu_more_option))
							}
						},
						state = rememberTooltipState()
					) {
						DropdownMenu(
							expanded = showDropDown,
							onDismissRequest = { showDropDown = false },
							shape = MaterialTheme.shapes.large,
						) {
							DropdownMenuItem(
								text = { Text(text = stringResource(id = R.string.menu_more_rename)) },
								enabled = isActionEnabled,
								onClick = {
									showDropDown = false
									onRenameOption(model)
								}
							)
							DropdownMenuItem(
								text = { Text(text = stringResource(id = R.string.menu_option_share)) },
								enabled = isActionEnabled,
								onClick = {
									showDropDown = false
									onShareOption()
								}
							)
							DropdownMenuItem(
								text = { Text(text = stringResource(id = R.string.menu_option_details)) },
								enabled = isActionEnabled,
								onClick = {
									showDropDown = false
									onDetailsOptions()
								}
							)
						}

						IconButton(onClick = { showDropDown = true }) {
							Icon(
								imageVector = Icons.Default.MoreVert,
								contentDescription = stringResource(R.string.menu_more_option)
							)
						}
					}
				},
				colors = colors,
				scrollBehavior = scrollBehavior,
				navigationIcon = navigation,
				modifier = modifier,
			)
		},
		onOther = {
			TopAppBar(
				title = {},
				colors = colors,
				scrollBehavior = scrollBehavior,
				navigationIcon = navigation,
				modifier = modifier,
			)
		},
	)

}

private fun favouriteAudioAnimation(): ContentTransform {
	val enter = fadeIn(animationSpec = tween(220)) +
			slideInVertically(animationSpec = tween(220))
	val exit = fadeOut(animationSpec = tween(90)) +
			slideOutVertically(animationSpec = tween(90))

	return enter togetherWith exit
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun AudioPlayerScreenTopBarPreview() = RecorderAppTheme {
	AudioPlayerScreenTopBar(
		state = ContentLoadState.Content(PlayerPreviewFakes.FAKE_AUDIO_MODEL),
		onEdit = {},
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = stringResource(R.string.back_arrow)
			)
		},
	)
}