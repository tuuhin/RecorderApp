package com.eva.recorderapp.voice_recorder.presentation.composables

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme

@Composable
fun IconButtonWithText(
	icon: @Composable () -> Unit,
	text: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	spacing: Dp = 4.dp,
	enabled: Boolean = true,
	isSelected: Boolean = false,
	shape: Shape = MaterialTheme.shapes.medium,
	textStyle: TextStyle = MaterialTheme.typography.labelMedium,
	colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
	selectedColor: IconButtonColors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
	val indication = LocalIndication.current

	val containerColor =
		if (enabled && isSelected) selectedColor.containerColor
		else if (enabled) colors.containerColor
		else colors.disabledContainerColor

	val contentColor =
		if (enabled && isSelected) selectedColor.contentColor
		else if (enabled) colors.contentColor
		else colors.disabledContentColor

	Column(
		modifier = modifier
			.defaultMinSize(minWidth = 56.dp)
			.minimumInteractiveComponentSize()
			.clip(shape)
			.background(color = containerColor)
			.clickable(
				interactionSource = interactionSource,
				enabled = enabled,
				indication = indication,
				onClick = onClick,
				role = Role.Button
			),
	) {
		CompositionLocalProvider(LocalContentColor provides contentColor) {
			Column(
				modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
				verticalArrangement = Arrangement.spacedBy(spacing),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				icon()
				Text(
					text = text,
					style = textStyle
				)
			}
		}
	}
}

class IsIconEnabledPreviewParams : CollectionPreviewParameterProvider<Boolean>(listOf(false, true))


@PreviewLightDark
@Composable
private fun IconButtonWithTextPreview(
	@PreviewParameter(IsIconEnabledPreviewParams::class)
	enabled: Boolean,
) = RecorderAppTheme {
	Surface {
		IconButtonWithText(
			icon = {
				Icon(
					painter = painterResource(R.drawable.ic_category_label),
					contentDescription = "Key"
				)
			},
			text = "Icon key",
			onClick = {},
			enabled = enabled,
		)
	}
}

@PreviewLightDark
@Composable
private fun IconButtonWithTextSelectedPreview() = RecorderAppTheme {
	Surface {
		IconButtonWithText(
			icon = {
				Icon(
					painter = painterResource(R.drawable.ic_category_label),
					contentDescription = "Key"
				)
			},
			text = "Icon key",
			onClick = {},
			isSelected = true
		)
	}
}