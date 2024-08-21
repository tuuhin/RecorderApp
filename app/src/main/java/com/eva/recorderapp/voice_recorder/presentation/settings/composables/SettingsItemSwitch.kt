package com.eva.recorderapp.voice_recorder.presentation.settings.composables

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
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
	shadowElevation: Dp = 0.dp,
	tonalElevation: Dp = 0.dp,
) {
	ListItem(
		headlineContent = { Text(text = title) },
		supportingContent = { Text(text = text) },
		trailingContent = {
			Switch(checked = isSelected, onCheckedChange = onSelect)
		},
		leadingContent = leading,
		shadowElevation = shadowElevation,
		tonalElevation = tonalElevation,
		modifier = modifier
			.clip(shape = MaterialTheme.shapes.medium)
			.clickable(role = Role.Switch) { onSelect(!isSelected) },
	)
}

@PreviewLightDark
@Composable
private fun SettingsItemSwitchPreview() = RecorderAppTheme {
	SettingsItemWithSwitch(
		isSelected = true,
		title = "Title",
		text = "Supporting Text",
		onSelect = {}
	)
}