package com.eva.feature_player.composable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInBack
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eva.player.domain.model.PlayerPlayBackSpeed
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme

@Composable
fun PlayBackSpeedSelector(
	selectedSpeed: PlayerPlayBackSpeed,
	onSpeedSelected: (PlayerPlayBackSpeed) -> Unit,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp),
) {
	LazyVerticalGrid(
		columns = GridCells.Fixed(3),
		modifier = modifier,
		contentPadding = contentPadding,
		horizontalArrangement = Arrangement.spacedBy(16.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp),
		userScrollEnabled = false,
	) {
		item(span = { GridItemSpan(3) }) {
			Text(
				text = stringResource(id = R.string.player_action_speed),
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.secondary,
				textAlign = TextAlign.Center,
			)
		}
		itemsIndexed(
			items = PlayerPlayBackSpeed.entries,
			contentType = { _, _ -> PlayerPlayBackSpeed::class.simpleName },
		) { _, item ->
			AudioPlaybackSpeedCard(
				isSelected = selectedSpeed == item,
				playBackSpeed = item,
				onSpeedSelected = { onSpeedSelected(item) },
				modifier = Modifier.aspectRatio(1.5f)
			)
		}
	}
}

@Composable
private fun AudioPlaybackSpeedCard(
	playBackSpeed: PlayerPlayBackSpeed,
	onSpeedSelected: () -> Unit,
	isSelected: Boolean,
	modifier: Modifier = Modifier,
) {
	val color by animateColorAsState(
		targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
		else MaterialTheme.colorScheme.surfaceContainerHigh,
		animationSpec = tween(durationMillis = 100, easing = EaseInBack),
		label = "Selected Color animation"
	)

	Card(
		modifier = modifier,
		onClick = onSpeedSelected,
		shape = MaterialTheme.shapes.medium,
		colors = CardDefaults.cardColors(
			containerColor = color,
			contentColor = contentColorFor(backgroundColor = color)
		)
	) {
		Box(
			modifier = Modifier.fillMaxSize(),
			contentAlignment = Alignment.Center,
		) {
			Text(
				text = stringResource(id = R.string.player_playback_speed, playBackSpeed.speed),
				style = MaterialTheme.typography.titleMedium,
				letterSpacing = .1.sp,
				fontStyle = FontStyle.Italic,
			)
		}
	}
}

@PreviewLightDark
@Composable
private fun PlayBackSpeedSelectorPreview() = RecorderAppTheme {
	Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
		PlayBackSpeedSelector(
			selectedSpeed = PlayerPlayBackSpeed.NORMAL,
			onSpeedSelected = {},
			modifier = Modifier.fillMaxWidth(),
			contentPadding = PaddingValues(20.dp)
		)
	}
}