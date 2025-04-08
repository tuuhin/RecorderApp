package com.eva.feature_recordings.recordings.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.feature_recordings.util.RecordingsPreviewFakes
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.ui.R
import com.eva.ui.animation.SharedElementTransitionKeys
import com.eva.ui.animation.sharedBoundsWrapper
import com.eva.ui.theme.RecorderAppTheme
import com.eva.utils.LocalTimeFormats
import kotlinx.datetime.format

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun RecordingCard(
	music: RecordedVoiceModel,
	onItemClick: () -> Unit,
	onItemSelect: () -> Unit,
	modifier: Modifier = Modifier,
	isSelectable: Boolean = false,
	isSelected: Boolean = false,
	shape: Shape = MaterialTheme.shapes.large,
) {
	val context = LocalContext.current
	val isInspectionMode = LocalInspectionMode.current

	val otherAppText = remember(music.owner, context) {
		if (isInspectionMode || music.owner == context.packageName) return@remember null

		music.owner ?: context.getString(R.string.other_app_subtitle)
	}

	val cardColor = if (!isSelected) CardDefaults.elevatedCardColors()
	else CardDefaults.cardColors()

	ElevatedCard(
		colors = cardColor,
		shape = shape,
		elevation = CardDefaults.elevatedCardElevation(pressedElevation = 4.dp),
		modifier = modifier
			.recordingCardCombinedClick(isSelectable, onItemClick, onItemSelect)
			.sharedBoundsWrapper(
				key = SharedElementTransitionKeys.recordSharedEntryContainer(music.id),
				resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
				enter = fadeIn(animationSpec = tween(easing = EaseOut, durationMillis = 300)),
				exit = fadeOut(animationSpec = tween(easing = EaseOut, durationMillis = 300)),
			),
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(16.dp),
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier
				.fillMaxWidth()
				.padding(all = dimensionResource(id = R.dimen.card_padding)),
		) {
			Crossfade(
				targetState = isSelectable,
				animationSpec = tween(durationMillis = 400),
				label = "Radio Button Animation",
				modifier = Modifier.padding(8.dp)
			) { showSelectOption ->
				if (showSelectOption)
					RadioButton(
						selected = isSelected,
						onClick = onItemSelect,
						colors = RadioButtonDefaults
							.colors(selectedColor = MaterialTheme.colorScheme.secondary),
						modifier = Modifier.size(24.dp)
					)
				else
					Image(
						painter = painterResource(id = R.drawable.ic_microphone),
						contentDescription = null,
						colorFilter = ColorFilter
							.tint(color = MaterialTheme.colorScheme.primary),
						modifier = Modifier.size(24.dp)
					)

			}
			Column(
				modifier = Modifier.weight(1f),
				verticalArrangement = Arrangement.spacedBy(4.dp)
			) {
				Text(
					text = music.displayName,
					style = MaterialTheme.typography.titleMedium,
					color = MaterialTheme.colorScheme.primary,
					modifier = Modifier.sharedBoundsWrapper(
						key = SharedElementTransitionKeys.recordSharedEntryTitle(music.id)
					)
				)
				Text(
					text = music.durationAsLocaltime.format(LocalTimeFormats.NOTIFICATION_TIMER_TIME_FORMAT),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onBackground,
				)
				AnimatedVisibility(
					visible = isSelectable && otherAppText != null,
					enter = slideInVertically(),
					exit = slideOutVertically()
				) {
					otherAppText?.let { text ->
						Text(
							text = text,
							style = MaterialTheme.typography.labelMedium,
							color = MaterialTheme.colorScheme.tertiary
						)
					}
				}
			}
			Column(
				horizontalAlignment = Alignment.End,
				verticalArrangement = Arrangement.spacedBy(6.dp)
			) {
				AnimatedVisibility(
					visible = music.isFavorite,
					enter = scaleIn() + fadeIn(),
					exit = scaleOut() + fadeOut()
				) {
					Icon(
						painter = painterResource(R.drawable.ic_star_filled),
						contentDescription = stringResource(R.string.menu_option_favourite),
						tint = MaterialTheme.colorScheme.secondary,
						modifier = Modifier.size(20.dp)
					)
				}
				Text(
					text = music.recordedAt.format(LocalTimeFormats.RECORDING_RECORD_TIME_FORMAT),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onBackground
				)
			}
		}
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.recordingCardCombinedClick(
	isSelectable: Boolean,
	onItemClick: () -> Unit,
	onItemSelect: () -> Unit,
	clipShape: Shape = MaterialTheme.shapes.medium,
) = composed {

	val onItemClickUpdatedState by rememberUpdatedState(onItemClick)
	val onItemSelectUpdatedState by rememberUpdatedState(onItemSelect)

	val clickModifier = if (isSelectable) clickable(
		onClick = onItemSelectUpdatedState,
		onClickLabel = "Item Selected"
	)
	else combinedClickable(
		onClick = onItemClickUpdatedState,
		onLongClick = onItemSelectUpdatedState,
		onClickLabel = "Item Clicked",
		onLongClickLabel = "Item Selected"
	)

	clip(clipShape).then(clickModifier)
}

@PreviewLightDark
@Composable
private fun RecordingCardNormalPreview() = RecorderAppTheme {
	RecordingCard(
		music = RecordingsPreviewFakes.FAKE_VOICE_RECORDING_MODEL,
		onItemClick = {},
		onItemSelect = {},
		modifier = Modifier.fillMaxWidth()
	)
}

@PreviewLightDark
@Composable
private fun RecordingCardSelectModePreview() = RecorderAppTheme {
	RecordingCard(
		music = RecordingsPreviewFakes.FAKE_VOICE_RECORDING_MODEL,
		onItemClick = {},
		onItemSelect = {},
		isSelectable = true,
		modifier = Modifier.fillMaxWidth(),
	)
}


@PreviewLightDark
@Composable
private fun RecordingCardSelectedPreview() = RecorderAppTheme {
	RecordingCard(
		music = RecordingsPreviewFakes.FAKE_VOICE_RECORDING_MODEL.copy(isFavorite = true),
		isSelectable = true,
		isSelected = true,
		onItemClick = {},
		onItemSelect = {},
		modifier = Modifier.fillMaxWidth(),
	)
}


