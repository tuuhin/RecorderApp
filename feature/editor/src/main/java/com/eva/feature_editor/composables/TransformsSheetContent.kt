package com.eva.feature_editor.composables

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.eva.editor.domain.TransformationProgress
import com.eva.feature_editor.event.TransformationState
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme


@Composable
internal fun TransformsSheetContent(
	state: TransformationState,
	onExport: () -> Unit,
	onTransform: () -> Unit,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(10.dp)
) {

	val transformType by remember(state) {
		derivedStateOf {
			when {
				state.isTransforming -> TransformType.TRANSFORMING
				state.isExportFileReady -> TransformType.READY_FOR_EXPORT
				else -> TransformType.IDLE
			}
		}
	}


	Column(
		modifier = modifier
			.padding(contentPadding)
			.fillMaxWidth(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Bottom,
	) {
		Crossfade(
			targetState = transformType,
			animationSpec = tween(durationMillis = 100, easing = EaseIn),
			modifier = Modifier.defaultMinSize(minWidth = 140.dp, minHeight = 140.dp)
		) { type ->
			when (type) {
				TransformType.IDLE -> Image(
					painter = painterResource(R.drawable.ic_transformation),
					contentDescription = "Transformation",
					colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.secondary),
					modifier = Modifier.size(128.dp)
				)

				TransformType.READY_FOR_EXPORT -> Image(
					painter = painterResource(R.drawable.ic_success),
					contentDescription = "success icon",
					colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.secondary),
					modifier = Modifier.size(128.dp)
				)

				TransformType.TRANSFORMING -> TransformationProgressIndicator(progress = state.progress)
			}
		}
		Spacer(modifier = Modifier.height(4.dp))
		Crossfade(
			targetState = transformType,
			animationSpec = tween(durationMillis = 200, easing = EaseInOut)
		) { type ->
			val message = when (type) {
				TransformType.IDLE -> stringResource(R.string.transform_bottom_sheet_text)
				TransformType.TRANSFORMING -> stringResource(R.string.transforming_media_bottom_sheet_text)
				TransformType.READY_FOR_EXPORT -> stringResource(R.string.export_bottom_sheet_text)
			}
			Text(
				text = message,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				style = MaterialTheme.typography.labelMedium,
				textAlign = TextAlign.Center,
			)
		}
		HorizontalDivider(
			modifier = Modifier.padding(vertical = 4.dp),
			color = MaterialTheme.colorScheme.outline
		)

		val message = when (transformType) {
			TransformType.IDLE -> stringResource(R.string.action_transform)
			TransformType.TRANSFORMING -> stringResource(R.string.action_transforming)
			TransformType.READY_FOR_EXPORT -> stringResource(R.string.action_export)
		}

		Button(
			onClick = if (state.isExportFileReady) onExport else onTransform,
			shape = MaterialTheme.shapes.extraLarge,
			enabled = !state.isTransforming,
			modifier = Modifier.fillMaxWidth(),
			contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
		) {
			Text(
				text = message,
				style = MaterialTheme.typography.titleMedium
			)
		}
	}
}

@Composable
private fun TransformationProgressIndicator(
	progress: TransformationProgress,
	modifier: Modifier = Modifier,
	arcColor: Color = MaterialTheme.colorScheme.secondary,
	textStyle: TextStyle = MaterialTheme.typography.titleMedium,
	textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
	val textMeasurer = rememberTextMeasurer()

	Spacer(
		modifier = modifier
			.size(120.dp)
			.drawWithCache {

				val progressAmount = (progress as? TransformationProgress.Progress)?.amount ?: 0
				val rotateAmount = (progressAmount * 360f / 100).coerceIn(0f, 360f)

				onDrawBehind {
					drawArc(
						color = arcColor,
						startAngle = 90f,
						size = Size(width = size.width * .9f, height = size.height * .9f),
						sweepAngle = rotateAmount,
						useCenter = false,
						style = Stroke(cap = StrokeCap.Round, width = 10.dp.toPx())
					)

					val textResults = textMeasurer.measure("$progressAmount %", style = textStyle)

					val centerPos =
						center - with(textResults.size) { Offset(width / 2f, height / 2f) }

					drawText(
						textLayoutResult = textResults,
						topLeft = centerPos,
						color = textColor
					)
				}

			},
	)
}


private enum class TransformType {
	IDLE,
	TRANSFORMING,
	READY_FOR_EXPORT
}

private class TransformsSheetContentPreviewParams :
	CollectionPreviewParameterProvider<TransformationState>(
		listOf(
			TransformationState(),
			TransformationState(exportFileUri = ""),
			TransformationState(
				isTransforming = true,
				progress = TransformationProgress.Progress(10)
			)
		)
	)

@PreviewLightDark
@Composable
private fun TransformSheetContentPreview(
	@PreviewParameter(TransformsSheetContentPreviewParams::class)
	state: TransformationState
) = RecorderAppTheme {
	Surface {
		TransformsSheetContent(
			state = state,
			onExport = {},
			onTransform = {},
		)
	}
}