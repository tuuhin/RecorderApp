package com.eva.feature_recorder.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme

@Composable
fun RecorderTranscriptionsChip(
	transcriptions: String?,
	modifier: Modifier = Modifier,
	labelTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
	labelColor: Color = MaterialTheme.colorScheme.onSurface,
	fontFamily: FontFamily = FontFamily.Default,
) {
	AnimatedVisibility(
		visible = transcriptions != null && transcriptions.isNotEmpty(),
		enter = slideInVertically() + scaleIn(initialScale = .4f),
		exit = slideOutVertically() + fadeOut(),
		modifier = modifier,
	) {
		if (transcriptions == null) return@AnimatedVisibility
		Row(
			verticalAlignment = Alignment.Top,
			horizontalArrangement = Arrangement.spacedBy(4.dp)
		) {
			Box(
				modifier = Modifier.background(
					MaterialTheme.colorScheme.tertiaryContainer,
					MaterialTheme.shapes.small
				),
				contentAlignment = Alignment.Center
			) {
				Icon(
					painter = painterResource(R.drawable.ic_live_translate),
					contentDescription = "Translation",
					tint = MaterialTheme.colorScheme.tertiary,
					modifier = Modifier
						.size(24.dp)
						.padding(3.dp)
				)
			}
			FlowRow(
				verticalArrangement = Arrangement.spacedBy(2.dp),
				horizontalArrangement = Arrangement.spacedBy(6.dp),
				maxItemsInEachRow = 4
			) {
				transcriptions.split(" ").fastForEach { word ->
					Text(
						text = word,
						style = labelTextStyle,
						color = labelColor,
						maxLines = 1,
						overflow = TextOverflow.StartEllipsis,
						textAlign = TextAlign.Center,
						fontWeight = FontWeight.SemiBold,
						fontFamily = fontFamily,
					)
				}
			}
		}
	}
}

@Preview
@Composable
private fun RecorderTranscriptionChipPreview() = RecorderAppTheme {
	RecorderTranscriptionsChip(transcriptions = "Hello world")
}