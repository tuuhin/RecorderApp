package com.eva.feature_settings.composables.transcribe

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import com.eva.feature_settings.SettingsPreviewFakes
import com.eva.transcribe.domain.models.STTModel
import com.eva.transcribe.domain.models.STTModelState
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme
import com.eva.ui.theme.RoundedPolygonShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun STTModelOptionCard(
	sttModel: STTModel,
	onDownload: () -> Unit,
	onDelete: () -> Unit,
	onSelect: () -> Unit,
	onUnSelect: () -> Unit,
	onSelectInvalid: () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	shadowElevation: Dp = 0.dp,
	tonalElevation: Dp = 0.dp,
	isSelected: Boolean = false,
	contentPadding: PaddingValues = PaddingValues(12.dp),
	shape: Shape = MaterialTheme.shapes.large,
) {

	val context = LocalContext.current

	val formattedSize = remember(sttModel.size) {
		Formatter.formatShortFileSize(context, sttModel.size ?: 0)
	}

	var showDialog by remember { mutableStateOf(false) }

	val polygonShape = remember {
		RoundedPolygon.star(
			numVerticesPerRadius = 9,
			radius = .8f,
			rounding = CornerRounding(radius = .5f, smoothing = .4f)
		)
	}

	DeleteSTTModelFilesDialog(
		showDialog = showDialog,
		modelId = sttModel.modelId,
		formattedSize = formattedSize,
		onDismiss = { showDialog = false },
		onConfirm = {
			onDelete()
			showDialog = false
		},
	)

	Row(
		modifier = modifier, verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(12.dp)
	) {
		AnimatedVisibility(
			visible = isSelected,
			enter = slideInHorizontally() + scaleIn(),
			exit = slideOutHorizontally() + shrinkOut(),
		) {
			Box(
				modifier = Modifier
					.size(40.dp)
					.background(
						color = MaterialTheme.colorScheme.primaryContainer,
						shape = RoundedPolygonShape(polygon = polygonShape)
					),
				contentAlignment = Alignment.Center,
			) {
				Icon(
					painter = painterResource(R.drawable.ic_check_plain),
					contentDescription = "Model selected",
					tint = MaterialTheme.colorScheme.onPrimaryContainer
				)
			}
		}
		Surface(
			tonalElevation = tonalElevation,
			shadowElevation = shadowElevation,
			onClick = {
				if (sttModel.state != STTModelState.DOWNLOADED) onSelectInvalid()
				if (isSelected) onUnSelect() else onSelect()
			},
			color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
			contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
			shape = shape
		) {
			Row(
				modifier = Modifier.padding(contentPadding),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(12.dp)
			) {
				Column(
					modifier = Modifier
						.animateContentSize()
						.weight(1f)
						.padding(horizontal = 8.dp),
					verticalArrangement = Arrangement.spacedBy(4.dp)
				) {
					Text(
						text = buildAnnotatedString {
							append("ID :")
							withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
								append(sttModel.modelId)
							}
						},
						style = MaterialTheme.typography.titleMedium,
						color = if (enabled) MaterialTheme.colorScheme.onBackground
						else MaterialTheme.colorScheme.onSurfaceVariant
					)
					Row(
						modifier = Modifier.height(IntrinsicSize.Min),
						horizontalArrangement = Arrangement.spacedBy(4.dp)
					) {
						Text(
							text = buildAnnotatedString {
								append("Locale: ")
								append(sttModel.locale)
							},
							style = MaterialTheme.typography.labelMedium,
							color = if (enabled) MaterialTheme.colorScheme.onBackground
							else MaterialTheme.colorScheme.onSurfaceVariant
						)
						VerticalDivider(
							thickness = 2.dp,
							modifier = Modifier.padding(horizontal = 2.dp)
						)
						Text(
							text = buildAnnotatedString {
								append("Size: ")
								append(formattedSize)
							},
							style = MaterialTheme.typography.labelMedium,
							color = if (enabled) MaterialTheme.colorScheme.onBackground
							else MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
				}
				TooltipBox(
					positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
						positioning = TooltipAnchorPosition.Below
					),
					tooltip = {
						PlainTooltip {
							Text("Some state")
						}
					},
					state = rememberTooltipState()
				) {
					IconButton(
						onClick = {
							if (sttModel.state == STTModelState.UN_AVAILABLE) onDownload()
							else showDialog = true
						},
						enabled = sttModel.state != STTModelState.DOWNLOADING,
						colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
					) {
						when (sttModel.state) {
							STTModelState.UN_AVAILABLE, STTModelState.DOWNLOADING, STTModelState.UPDATE_AVAILABLE -> Icon(
								painter = painterResource(R.drawable.ic_download_rounded),
								contentDescription = "Model downloadable"
							)

							STTModelState.DOWNLOADED -> Icon(
								painter = painterResource(R.drawable.ic_checked_batch),
								contentDescription = "Model already present"
							)
						}
					}
				}
			}
		}
	}
}


@Preview
@Composable
private fun STTModelOptionCardPreview() = RecorderAppTheme {
	STTModelOptionCard(
		isSelected = true,
		sttModel = SettingsPreviewFakes.STT_MODEL,
		onDownload = { },
		onDelete = { },
		onSelect = {},
		onUnSelect = {},
		onSelectInvalid = { },
	)
}