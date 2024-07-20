package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme

@Composable
fun RecordButton(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	contentDesciption: String? = null,
	elevation: Dp = 4.dp,
) {
	val recorderRed = colorResource(id = R.color.recorder_red)

	Spacer(
		modifier = modifier
			.semantics {
				contentDescription = contentDesciption ?: "RECORD_BUTTON"
			}
			.size(64.dp)
			.clip(CircleShape)
			.clickable(onClick = onClick, role = Role.Button)
			.shadow(elevation = elevation)
			.drawBehind {
				drawCircle(color = recorderRed)
				drawCircle(color = Color.White, style = Stroke(width = 6.dp.toPx()))
			}
	)
}


@PreviewLightDark
@Composable
private fun RecordButtonPreview() = RecorderAppTheme {
	Surface {
		RecordButton(onClick = {}, modifier = Modifier.padding(24.dp))
	}
}