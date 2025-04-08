package com.eva.feature_settings.composables

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun SettingsItemWithSwitch(
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
	ListItem(
		headlineContent = {
			Text(
				text = title,
				style = MaterialTheme.typography.titleMedium,
				color = if (enabled) MaterialTheme.colorScheme.onBackground
				else MaterialTheme.colorScheme.onSurfaceVariant
			)
		},
		supportingContent = {
			Text(
				text = text,
				style = MaterialTheme.typography.labelMedium,
				color = if (enabled) MaterialTheme.colorScheme.onBackground
				else MaterialTheme.colorScheme.onSurfaceVariant
			)
		},
		trailingContent = {
			Switch(
				checked = isSelected,
				enabled = enabled,
				onCheckedChange = onSelect,
				colors = SwitchDefaults.colors(
					checkedTrackColor = MaterialTheme.colorScheme.secondary,
					checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
				)
			)
		},
		leadingContent = leading,
		modifier = modifier
			.clip(shape = MaterialTheme.shapes.medium)
			.clickable { onSelect(!isSelected) },
		tonalElevation = tonalElevation,
		shadowElevation = shadowElevation,
		colors = ListItemDefaults.colors(containerColor = Color.Transparent)
	)
}

@PreviewLightDark
@Composable
private fun SettingsItemSwitchPreview() = RecorderAppTheme {
	SettingsItemWithSwitch(
		isSelected = false,
		title = "Title",
		text = "Supporting Text",
		onSelect = { },
	)
}