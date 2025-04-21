package com.eva.feature_editor.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.eva.feature_editor.event.AudioClipConfig
import com.eva.ui.R
import kotlin.time.Duration

@Composable
fun EditorTrimOverlay(
	trackDuration: Duration,
	modifier: Modifier = Modifier,
	clipConfig: AudioClipConfig? = null,
	overlayColor: Color = MaterialTheme.colorScheme.tertiary,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	shape: Shape = MaterialTheme.shapes.medium,
) {

	val cutPainter = painterResource(R.drawable.ic_cut)

	Spacer(
		modifier = modifier
			.padding(contentPadding)
			.drawWithCache {

				val (startRatio, endRatio) = clipConfig?.let { config ->
					(config.start / trackDuration).toFloat() to
							(config.end / trackDuration).toFloat()
				} ?: (0f to 1f)


				val topLeftCorner = Offset(startRatio * size.width, 0f)
				val bottomRightCorner = Offset(endRatio * size.width, size.height)

				val overlaySize = Size(
					width = bottomRightCorner.x - topLeftCorner.x,
					height = bottomRightCorner.y - topLeftCorner.y
				)
				val overlayOutline =
					shape.createOutline(overlaySize, layoutDirection, density = this)

				onDrawBehind {
					translate(left = topLeftCorner.x - 9.dp.toPx()) {
						// draw a rounded rect
						drawOutline(outline = overlayOutline, color = overlayColor, alpha = 0.4f)
						// and a border
						drawOutline(
							outline = overlayOutline,
							color = overlayColor,
							alpha = 0.4f,
							style = Stroke(width = 2.dp.toPx())
						)
						// marks
						with(cutPainter) {
							// marker at start
							translate(left = -9.dp.toPx(), top = overlaySize.height + 4.dp.toPx()) {
								draw(
									size = Size(18.dp.toPx(), 18.dp.toPx()),
									colorFilter = ColorFilter.tint(color = overlayColor)
								)
							}
							translate(
								left = overlaySize.width - 9.dp.toPx(),
								top = overlaySize.height + 4.dp.toPx()
							) {
								draw(
									size = Size(18.dp.toPx(), 18.dp.toPx()),
									colorFilter = ColorFilter.tint(color = overlayColor)
								)
							}
						}
					}
				}
			},
	)
}
