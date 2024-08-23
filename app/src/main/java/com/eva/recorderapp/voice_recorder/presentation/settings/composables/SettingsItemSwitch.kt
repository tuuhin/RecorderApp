package com.eva.recorderapp.voice_recorder.presentation.settings.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.ui.theme.RecorderAppTheme

@Composable
fun SettingsItemWithSwitch(
	isSelected: Boolean,
	title: String,
	text: String,
	onSelect: (Boolean) -> Unit,
	modifier: Modifier = Modifier,
	leading: (@Composable () -> Unit)? = null,
	enabled: Boolean = true,
	shadowElevation: Dp = 0.dp,
	tonalElevation: Dp = 0.dp,
) {
	Surface(
		onClick = { onSelect(!isSelected) },
		enabled = enabled,
		tonalElevation = tonalElevation,
		shadowElevation = shadowElevation,
		shape = MaterialTheme.shapes.medium,
		modifier = modifier.fillMaxWidth()
	) {
		Row(
			modifier = Modifier.padding(8.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			leading?.let { content ->
				Box(
					modifier = Modifier
						.defaultMinSize(32.dp, 32.dp)
						.padding(horizontal = 12.dp),
					contentAlignment = Alignment.Center
				) {
					content()
				}
			}
			Column(
				modifier = Modifier.weight(1f)
			) {
				Text(
					text = title,
					style = MaterialTheme.typography.bodyLarge,
					color = if (enabled) MaterialTheme.colorScheme.onBackground
					else MaterialTheme.colorScheme.onSurfaceVariant
				)
				Text(
					text = text,
					style = MaterialTheme.typography.labelLarge,
					color = if (enabled) MaterialTheme.colorScheme.onBackground
					else MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
			Switch(
				checked = isSelected,
				enabled = enabled,
				onCheckedChange = onSelect,
				colors = SwitchDefaults.colors(
					checkedTrackColor = MaterialTheme.colorScheme.secondary,
					checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
				)
			)
		}
	}
}

@PreviewLightDark
@Composable
private fun SettingsItemSwitchPreview() = RecorderAppTheme {

	SettingsItemWithSwitch(isSelected = false,
		title = "Title",
		text = "Supporting Text",
		onSelect = { },
		leading = {
			Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null)
		})

}