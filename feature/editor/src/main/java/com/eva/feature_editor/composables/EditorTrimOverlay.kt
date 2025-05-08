package com.eva.feature_editor.composables

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.eva.editor.domain.model.AudioClipConfig
import com.eva.player_shared.util.PlayerGraphData
import com.eva.ui.R
import kotlin.time.Duration

@Composable
internal fun Modifier.trimOverlay(
	graph: PlayerGraphData,
	trackDuration: Duration,
	maxGraphPoints: Int = 100,
	clipConfig: AudioClipConfig? = null,
	overlayColor: Color = MaterialTheme.colorScheme.tertiary,
	shape: Shape = MaterialTheme.shapes.medium,
) = composed {

	val cutPainter = painterResource(R.drawable.ic_cut)

	defaultMinSize(minHeight = dimensionResource(id = R.dimen.line_graph_min_height))
		.drawWithCache {

			val (startRatio, endRatio) = clipConfig?.let { config ->
				(config.start / trackDuration).toFloat() to
						(config.end / trackDuration).toFloat()
			} ?: (0f to 1f)

			val pathEffect = PathEffect.dashPathEffect(
				intervals = floatArrayOf(10.dp.toPx(), 10.dp.toPx())
			)
			val eachPointSize = size.width / maxGraphPoints

			onDrawBehind {

				val samples = graph()
				val isCompressedGraph = samples.size >= maxGraphPoints

				val width = if (isCompressedGraph) size.width
				else samples.size * eachPointSize


				val topLeftCorner = Offset(startRatio * width, 0f)
				val bottomRightCorner = Offset(endRatio * width, size.height)


				val overlaySize = Size(
					width = bottomRightCorner.x - topLeftCorner.x,
					height = bottomRightCorner.y - topLeftCorner.y
				)
				val overlayOutline = shape.createOutline(overlaySize, layoutDirection, this)

				translate(left = topLeftCorner.x) {
					// draw a rounded rect
					drawOutline(
						outline = overlayOutline,
						color = overlayColor,
						alpha = 0.2f,
					)
					// and a border
					drawOutline(
						outline = overlayOutline,
						color = overlayColor,
						style = Stroke(
							width = 2.dp.toPx(),
							pathEffect = pathEffect,
							cap = StrokeCap.Round
						),
						alpha = .6f,
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
		}

}
