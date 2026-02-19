package com.eva.feature_settings.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsItemTitle(
	title: String,
	modifier: Modifier = Modifier,
	text: String? = null,
	titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
	titleColor: Color = MaterialTheme.colorScheme.primary,
	supportingTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
	supportingTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
	contentPadding: PaddingValues = PaddingValues.Zero,
) {
	Column(
		modifier = modifier
			.padding(contentPadding)
			.wrapContentHeight(),
		verticalArrangement = Arrangement.spacedBy(4.dp)
	) {
		Text(
			text = title,
			style = titleStyle,
			color = titleColor
		)
		text?.let {
			Text(
				text = text,
				style = supportingTextStyle,
				color = supportingTextColor
			)
		}
	}
}