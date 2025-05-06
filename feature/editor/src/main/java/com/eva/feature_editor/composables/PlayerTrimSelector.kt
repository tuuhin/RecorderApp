package com.eva.feature_editor.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.eva.editor.domain.model.AudioClipConfig
import com.eva.player.domain.model.PlayerTrackData
import com.eva.player_shared.util.PlayerGraphData
import com.eva.player_shared.util.PlayerPreviewFakes
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme
import com.eva.utils.RecorderConstants

@Composable
fun PlayerTrimSelector(
	graphData: PlayerGraphData,
	trackData: PlayerTrackData,
	onClipConfigChange: (AudioClipConfig) -> Unit,
	modifier: Modifier = Modifier,
	clipConfig: AudioClipConfig? = null,
	maxGraphPoints: Int = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE,
	shape: Shape = MaterialTheme.shapes.small,
	overlayColor: Color = MaterialTheme.colorScheme.tertiary,
	containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
	contentPadding: PaddingValues = PaddingValues(
		horizontal = dimensionResource(id = R.dimen.graph_card_padding),
		vertical = dimensionResource(id = R.dimen.graph_card_padding_other)
	),
) {
	Surface(
		color = containerColor,
		shape = shape,
		modifier = modifier.aspectRatio(1.7f),
	) {
		Box(modifier = Modifier.padding(contentPadding)) {
			EditorAmplitudeGraph(
				playRatio = { trackData.playRatio },
				totalTrackDuration = trackData.total,
				maxGraphPoints = maxGraphPoints,
				graphData = graphData,
				modifier = Modifier
					.matchParentSize()
					.trimOverlay(
						graph = graphData,
						trackDuration = trackData.total,
						clipConfig = clipConfig,
						maxGraphPoints = maxGraphPoints,
						overlayColor = overlayColor,
						shape = shape,
					)
					.detectClipConfig(
						graph = graphData,
						onClipChange = onClipConfigChange,
						totalLength = trackData.total,
						clipConfig = clipConfig,
						maxGraphPoints = maxGraphPoints
					),
			)
		}
	}
}


@Preview
@Composable
private fun PlayerTrimSelectorPreview() = RecorderAppTheme {
	PlayerTrimSelector(
		graphData = { PlayerPreviewFakes.PREVIEW_RECORDER_AMPLITUDES },
		trackData = PlayerPreviewFakes.FAKE_TRACK_DATA,
		onClipConfigChange = {}
	)
}