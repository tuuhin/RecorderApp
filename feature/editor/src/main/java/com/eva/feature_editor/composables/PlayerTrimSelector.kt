package com.eva.feature_editor.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eva.editor.domain.model.AudioClipConfig
import com.eva.player.domain.model.PlayerTrackData
import com.eva.player_shared.util.PlayerGraphData
import com.eva.player_shared.util.PlayerPreviewFakes
import com.eva.ui.theme.DownloadableFonts
import com.eva.ui.theme.RecorderAppTheme
import com.eva.utils.RecorderConstants

@Composable
internal fun PlayerTrimSelector(
	graphData: PlayerGraphData,
	trackData: () -> PlayerTrackData,
	onClipConfigChange: (AudioClipConfig) -> Unit,
	modifier: Modifier = Modifier,
	clipConfig: AudioClipConfig? = null,
	enabled: Boolean = true,
	maxGraphPoints: Int = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE,
	shape: Shape = MaterialTheme.shapes.large,
	overlayColor: Color = MaterialTheme.colorScheme.tertiary,
	containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
	contentPadding: PaddingValues = PaddingValues(28.dp),
) {
	val totalTrackDuration by remember { derivedStateOf { trackData().total } }

	Surface(
		color = containerColor,
		shape = shape,
		modifier = modifier.aspectRatio(1.7f),
	) {
		EditorAmplitudeGraph(
			playRatio = { trackData().playRatio },
			totalTrackDuration = totalTrackDuration,
			maxGraphPoints = maxGraphPoints,
			graphData = graphData,
			timelineFontFamily = DownloadableFonts.PLUS_CODE_LATIN_FONT_FAMILY,
			modifier = Modifier
				.padding(contentPadding)
				.trimOverlay(
					graph = graphData,
					enabled = enabled,
					trackDuration = totalTrackDuration,
					clipConfig = clipConfig,
					maxGraphPoints = maxGraphPoints,
					overlayColor = overlayColor,
					shape = shape,
				)
				.detectClipConfig(
					graph = graphData,
					enabled = enabled,
					onClipChange = onClipConfigChange,
					totalLength = totalTrackDuration,
					clipConfig = clipConfig,
					maxGraphPoints = maxGraphPoints
				)
		)
	}
}


@Preview
@Composable
private fun PlayerTrimSelectorPreview() = RecorderAppTheme {
	PlayerTrimSelector(
		graphData = { PlayerPreviewFakes.PREVIEW_RECORDER_AMPLITUDES },
		trackData = { PlayerPreviewFakes.FAKE_TRACK_DATA },
		onClipConfigChange = {}
	)
}