package com.eva.feature_player.views

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextPaint
import androidx.core.graphics.withTranslation
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

fun Canvas.drawGraph(
	waves: FloatArray,
	spikesGap: Float = 2f,
	spikesWidth: Float = 2f,
	color: Int,
	drawPoints: Boolean = true,
	startIdx: Int = 0,
	topPadding: Int = 0,
	bottomPadding: Int = 0,
	leftPadding: Int = 0,
) {
	val totalVPadding = topPadding + bottomPadding
	val centerYAxis = (height - totalVPadding) * 0.5f
	val paint = Paint().apply {
		this.color = color
		this.strokeWidth = spikesGap
		strokeCap = Paint.Cap.ROUND
		isAntiAlias = true
	}

	for ((idx, value) in waves.withIndex()) {
		val actualIndex = startIdx + idx
		val sizeFactor = value * .8f
		val xAxis = leftPadding + spikesWidth * actualIndex
		val startY = centerYAxis * (1 - sizeFactor)
		val endY = centerYAxis * (1 + sizeFactor)

		if (startY != endY) {
			drawLine(
				xAxis,
				totalVPadding * .5f + startY,
				xAxis,
				totalVPadding * .5f + endY,
				paint
			)
		} else if (drawPoints) {
			drawCircle(
				xAxis,
				totalVPadding * .5f + centerYAxis,
				spikesGap / 2f,
				paint
			)
		}
	}
}

fun Canvas.drawTimeLine(
	totalDurationInMillis: Long,
	textSizeInSp: Float,
	outlineColor: Int = Color.GRAY,
	outlineVariant: Int = Color.GRAY,
	spikesWidth: Float = 2f,
	strokeWidthThick: Float = 2f,
	strokeWidthLight: Float = 1f,
	textColor: Int = Color.BLACK,
	sampleSize: Int = 100,
	dpToPx: (Float) -> Float,
	typeface: Typeface = Typeface.MONOSPACE,
	topPadding: Int = 0,
	bottomPadding: Int = 0,
	leftPadding: Int = 0,
) {
	val durationAsMillis = (totalDurationInMillis + 2 * 1_000).toInt()
	val spacing = spikesWidth / sampleSize

	val paintThick = Paint().apply {
		color = outlineColor
		strokeWidth = strokeWidthThick
		strokeCap = Paint.Cap.ROUND
		isAntiAlias = true
	}

	val paintLight = Paint().apply {
		color = outlineVariant
		strokeWidth = strokeWidthLight
		strokeCap = Paint.Cap.ROUND
		isAntiAlias = true
	}

	val textPaint = TextPaint().apply {
		color = textColor
		this.textSize = textSizeInSp
		isAntiAlias = true
		textAlign = Paint.Align.CENTER
		this.typeface = typeface
	}

	repeat(durationAsMillis) { millis ->
		val xAxis = millis * spacing + leftPadding
		if (millis % 2000 == 0) {
			val time = millis.milliseconds
			val minutes = time.inWholeMinutes
			val seconds = (time.inWholeSeconds % 60)
			val readable = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

			val textY = dpToPx(8f).unaryMinus() - (textPaint.descent() + textPaint.ascent()) / 2
			drawText(readable, xAxis, topPadding + textY, textPaint)

			drawLine(xAxis, topPadding.toFloat(), xAxis, topPadding + dpToPx(8f), paintThick)
			drawLine(
				xAxis,
				height - dpToPx(8f) - bottomPadding,
				xAxis,
				height.toFloat() - bottomPadding,
				paintThick
			)
		} else if (millis % 500 == 0) {
			drawLine(xAxis, topPadding.toFloat(), xAxis, topPadding + dpToPx(4f), paintLight)
			drawLine(
				xAxis,
				height - dpToPx(4f) - bottomPadding,
				xAxis,
				height.toFloat() - bottomPadding,
				paintLight
			)
		}
	}
}

fun Canvas.drawTimeLineWithBookMarks(
	totalDurationInMillis: Long,
	bookMarks: Iterable<Int>,
	dpToPx: (Float) -> Float,
	imageSize: Float = 20f,
	textSizeInSp: Float = 16f,
	bookMarkDrawable: Drawable? = null,
	sampleSize: Int = 100,
	outlineColor: Int = Color.WHITE,
	outlineVariant: Int = Color.WHITE,
	bookMarkColor: Int = Color.WHITE,
	spikesWidth: Float = 2f,
	strokeWidthThick: Float = 2f,
	strokeWidthLight: Float = 1f,
	bookMarkStokeWidth: Float = 2f,
	textColor: Int = Color.WHITE,
	typeface: Typeface = Typeface.MONOSPACE,
	leftPadding: Int = 0,
	topPadding: Int = 0,
	bottomPadding: Int = 0,
) {
	drawTimeLine(
		totalDurationInMillis = totalDurationInMillis,
		sampleSize = sampleSize,
		outlineColor = outlineColor,
		outlineVariant = outlineVariant,
		strokeWidthThick = strokeWidthThick,
		strokeWidthLight = strokeWidthLight,
		textColor = textColor,
		textSizeInSp = textSizeInSp,
		spikesWidth = spikesWidth,
		dpToPx = dpToPx,
		typeface = typeface,
		topPadding = topPadding,
		bottomPadding = bottomPadding,
		leftPadding = leftPadding,
	)

	val spacing = spikesWidth / sampleSize

	val bookMarkPaint = Paint().apply {
		color = bookMarkColor
		strokeWidth = bookMarkStokeWidth
		strokeCap = Paint.Cap.ROUND
		isAntiAlias = true
		style = Paint.Style.FILL
	}

	bookMarks.forEach { timeInMillis ->
		val xAxis = timeInMillis * spacing + leftPadding

		// Draw vertical line
		drawLine(
			xAxis,
			topPadding + dpToPx(2f),
			xAxis,
			height - dpToPx(2f) - bottomPadding,
			bookMarkPaint
		)
		// Draw circle at top
		drawCircle(xAxis, topPadding + dpToPx(2f), dpToPx(3f), bookMarkPaint)

		// Draw bookmark icon
		bookMarkDrawable?.let { drawable ->
			withTranslation(xAxis - (imageSize / 2f), height + dpToPx(4f) - bottomPadding) {
				drawable.setBounds(0, 0, imageSize.toInt(), imageSize.toInt())
				drawable.setTint(bookMarkColor)
				drawable.draw(this)
			}
		}
	}
}

fun Canvas.drawTrackPointer(
	xAxis: Float,
	color: Int,
	radius: Float = 1f,
	strokeWidth: Float = 1f,
	topPadding: Int = 0,
	bottomPadding: Int = 0,
) {
	val paint = Paint().apply {
		this.color = color
		this.strokeWidth = strokeWidth
		strokeCap = Paint.Cap.ROUND
		isAntiAlias = true
		style = Paint.Style.FILL
	}

	// Draw circles
	drawCircle(xAxis, topPadding.toFloat(), radius, paint)
	drawCircle(xAxis, (height - bottomPadding).toFloat(), radius, paint)

	// Draw line
	paint.style = Paint.Style.STROKE
	drawLine(xAxis, topPadding.toFloat(), xAxis, (height - bottomPadding).toFloat(), paint)
}