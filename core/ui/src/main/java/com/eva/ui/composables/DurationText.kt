package com.eva.ui.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.toJavaDuration

@Composable
fun DurationText(
	duration: Duration,
	modifier: Modifier = Modifier,
	style: TextStyle = MaterialTheme.typography.bodyMedium,
	formatToLocale: Boolean = true,
	fontFamily: FontFamily? = null,
	fontWeight: FontWeight? = null,
) {
	val locale = remember(formatToLocale) { if (formatToLocale) Locale.getDefault() else Locale.US }

	val formattedDuration = remember(duration) {
		val pattern = when {
			duration.inWholeHours > 0 -> "HH:mm:ss"
			else -> "mm:ss"
		}
		val formatter = DateTimeFormatter.ofPattern(pattern, locale)
		val instant = Instant.EPOCH.plus(duration.toJavaDuration())
		formatter.format(instant.atOffset(ZoneOffset.UTC))
	}
	Text(
		text = formattedDuration,
		modifier = modifier,
		style = style,
		fontFamily = fontFamily,
		fontWeight = fontWeight
	)
}
