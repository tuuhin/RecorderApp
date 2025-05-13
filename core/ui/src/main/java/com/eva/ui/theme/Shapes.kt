package com.eva.ui.theme

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import kotlin.math.max

class RoundedPolygonShape(
	private val polygon: RoundedPolygon,
	private val rotation: Float = 0f,
	private var matrix: Matrix = Matrix(),
) : Shape {

	private var path = Path()
	override fun createOutline(
		size: Size,
		layoutDirection: LayoutDirection,
		density: Density,
	): Outline {
		path.rewind()
		path = polygon.toPath().asComposePath()
		matrix.reset()

		val bounds = polygon.calculateBounds()
			.let { Rect(it.get(0), it.get(1), it.get(2), it.get(3)) }

		val maxDimension = max(bounds.width, bounds.height)
		matrix.scale(size.width / maxDimension, size.height / maxDimension)
		matrix.translate(-bounds.left, -bounds.top)
		matrix.rotateZ(rotation)

		path.transform(matrix)
		return Outline.Generic(path)
	}
}

object CustomShapes {
	val ROUNDED_STAR = RoundedPolygon.star(
		numVerticesPerRadius = 6,
		rounding = CornerRounding(radius = 4f),
		innerRadius = .6f
	)

	val ROUNDED_STAR_8_CORNERS =  RoundedPolygon.star(
		numVerticesPerRadius = 8,
		rounding = CornerRounding(radius = 4f),
		innerRadius = .6f
	)
}
