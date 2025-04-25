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
import com.eva.feature_editor.event.AudioClipConfig
import com.eva.player.domain.model.PlayerTrackData
import com.eva.player_shared.util.PlayerPreviewFakes
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme

@Composable
fun PlayerTrimSelector(
	graphData: () -> List<Float>,
	trackData: PlayerTrackData,
	onClipConfigChange: (AudioClipConfig) -> Unit,
	modifier: Modifier = Modifier,
	clipConfig: AudioClipConfig? = null,
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
		Box(
			modifier = Modifier
				.padding(contentPadding)
				.detectClipConfig(
					onClipChange = onClipConfigChange,
					totalLength = trackData.total,
					clipConfig = clipConfig
				),
		) {
			EditorAmplitudeGraph(
				playRatio = { trackData.playRatio },
				totalTrackDuration = trackData.total,
				graphData = graphData,
				modifier = Modifier.matchParentSize(),
			)
			EditorTrimOverlay(
				trackDuration = trackData.total,
				modifier = Modifier.matchParentSize(),
				clipConfig = clipConfig,
				overlayColor = overlayColor,
				shape = shape,
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