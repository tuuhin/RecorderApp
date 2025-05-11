package com.eva.feature_player.composable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInBack
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

	val entries = remember {
		listOf(
			PlayerPlayBackSpeed.VerySlow,
			PlayerPlayBackSpeed.Slow,
			PlayerPlayBackSpeed.Normal,
			PlayerPlayBackSpeed.Fast,
			PlayerPlayBackSpeed.VeryFast,
			PlayerPlayBackSpeed.VeryVeryFast
		)
	}

	Column(
		modifier = modifier
			.fillMaxWidth()
			.padding(contentPadding),
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		Text(
			text = stringResource(id = R.string.player_action_speed),
			style = MaterialTheme.typography.titleLarge,
			color = MaterialTheme.colorScheme.onSurface,
			textAlign = TextAlign.Center,
			modifier = Modifier.align(Alignment.CenterHorizontally)
		)
		Spacer(modifier = Modifier.height(2.dp))
		Text(
			text = stringResource(id = R.string.player_playback_speed, selectedSpeed.speed),
			modifier = Modifier.align(Alignment.CenterHorizontally),
			style = MaterialTheme.typography.titleMedium,
			color = MaterialTheme.colorScheme.onSurface,
			textAlign = TextAlign.Center,
		)
		Row(
			horizontalArrangement = Arrangement.spacedBy(4.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			IconButton(
				onClick = {
					val newAmount = selectedSpeed.speed - .1f
					if (newAmount > 0f)
						onSpeedSelected(PlayerPlayBackSpeed.CustomSpeed(newAmount))
				},
			) {
				Icon(
					painter = painterResource(R.drawable.ic_minus),
					contentDescription = "Reduce speed",
					modifier = Modifier.size(24.dp),
				)
			}
			Slider(
				value = selectedSpeed.speed.coerceIn(.0f..2f),
				onValueChange = { onSpeedSelected(PlayerPlayBackSpeed.CustomSpeed(it)) },
				valueRange = .0f..2f,
				modifier = Modifier.weight(1f)
			)
			IconButton(
				onClick = {
					val newAmount = selectedSpeed.speed + .1f
					if (newAmount <= 2f)
						onSpeedSelected(PlayerPlayBackSpeed.CustomSpeed(newAmount))
				},
			) {
				Icon(
					painter = painterResource(R.drawable.ic_plus),
					contentDescription = "Increase speed",
					modifier = Modifier.size(24.dp),
				)
			}
		}
		LazyVerticalGrid(
			columns = GridCells.Fixed(3),
			horizontalArrangement = Arrangement.spacedBy(16.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp),
			userScrollEnabled = false,
		) {
			itemsIndexed(
				items = entries,
				contentType = { _, _ -> PlayerPlayBackSpeed::class.simpleName },
			) { _, item ->

				val isSelected = selectedSpeed.speed == item.speed

				AudioPlaybackSpeedCard(
					isSelected = isSelected,
					playBackSpeed = item,
					onSpeedSelected = { onSpeedSelected(item) },
				)
			}
		}
	}
}

@Composable
private fun AudioPlaybackSpeedCard(
	playBackSpeed: PlayerPlayBackSpeed,
	onSpeedSelected: () -> Unit,
	isSelected: Boolean = false,
	modifier: Modifier = Modifier,
	selectedColor: Color = MaterialTheme.colorScheme.primary,
	unselectedColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
	val color by animateColorAsState(
		targetValue = if (isSelected) selectedColor else unselectedColor,
		animationSpec = tween(durationMillis = 100, easing = EaseInBack),
		label = "Selected Color animation"
	)

	Box(
		modifier = modifier
			.defaultMinSize(minHeight = 40.dp)
			.clip(MaterialTheme.shapes.large)
			.drawBehind {
				drawRect(color = color)
			}
			.clickable(onClick = onSpeedSelected),
		contentAlignment = Alignment.Center
	) {
		Text(
			text = stringResource(id = R.string.player_playback_speed, playBackSpeed.speed),
			style = MaterialTheme.typography.titleMedium,
			letterSpacing = .1.sp,
			color = contentColorFor(backgroundColor = if (isSelected) selectedColor else unselectedColor)
		)
	}
}

@PreviewLightDark
@Composable
private fun PlayBackSpeedSelectorPreview() = RecorderAppTheme {
	Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
		PlayBackSpeedSelector(
			selectedSpeed = PlayerPlayBackSpeed.Fast,
			onSpeedSelected = {},
			modifier = Modifier.fillMaxWidth(),
			contentPadding = PaddingValues(20.dp)
		)
	}
}