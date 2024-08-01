package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordButton(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	elevation: Dp = 4.dp,
) {
	val recorderRed = colorResource(id = R.color.recorder_button_color)
	val rimColor = colorResource(id = R.color.recorder_button_rim_color)
	val contentDesciption = stringResource(id = R.string.recorder_action_start)

	val interactionSource = remember {
		MutableInteractionSource()
	}

	TooltipBox(
		positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
		tooltip = {
			PlainTooltip {
				Text(text = stringResource(id = R.string.recorder_action_start))
			}
		},
		state = rememberTooltipState(),
	) {
		Spacer(
			modifier = modifier
				.indication(interactionSource = interactionSource, indication = rememberRipple())
				.semantics {
					contentDescription = contentDesciption
				}
				.size(64.dp)
				.clip(CircleShape)
				.clickable(onClick = onClick, role = Role.Button)
				.shadow(elevation = elevation)
				.drawBehind {
					drawCircle(color = recorderRed)
					drawCircle(color = rimColor, style = Stroke(width = 6.dp.toPx()))
				}
		)
	}
}


@PreviewLightDark
@Composable
private fun RecordButtonPreview() = RecorderAppTheme {
	Surface {
		RecordButton(onClick = {}, modifier = Modifier.padding(24.dp))
	}
}