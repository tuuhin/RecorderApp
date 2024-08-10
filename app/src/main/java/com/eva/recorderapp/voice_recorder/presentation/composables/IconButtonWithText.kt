package com.eva.recorderapp.voice_recorder.presentation.composables

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.ui.theme.RecorderAppTheme

@Composable
fun IconButtonWithText(
	icon: @Composable () -> Unit,
	text: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	spacing: Dp = 4.dp,
	enabled: Boolean = true,
	shape: Shape = MaterialTheme.shapes.medium,
	textStyle: TextStyle = MaterialTheme.typography.labelMedium,
	colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {

	val indication = LocalIndication.current

	Column(
		modifier = modifier
			.defaultMinSize(minWidth = 56.dp)
			.minimumInteractiveComponentSize()
			.clip(shape)
			.background(
				color = if (enabled) colors.containerColor else colors.disabledContainerColor
			)
			.clickable(
				interactionSource = interactionSource,
				enabled = enabled,
				indication = indication,
				onClick = onClick,
				role = Role.Button
			),
	) {
		Column(
			modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
			verticalArrangement = Arrangement.spacedBy(spacing),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			icon()
			Text(
				text = text,
				style = textStyle,
				color = if (enabled) colors.contentColor else colors.disabledContentColor
			)
		}
	}
}


@PreviewLightDark
@Composable
private fun IconButtonWithTextPreview() = RecorderAppTheme {
	Surface {
		IconButtonWithText(
			icon = { Icon(imageVector = Icons.Default.Key, contentDescription = "Key") },
			text = "Icon key",
			onClick = {}
		)
	}
}