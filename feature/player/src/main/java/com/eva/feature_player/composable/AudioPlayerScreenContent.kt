package com.eva.feature_player.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eva.bookmarks.domain.AudioBookmarkModel
import com.eva.feature_player.bookmarks.state.BookMarkEvents
import com.eva.feature_player.bookmarks.state.CreateBookmarkState
import com.eva.feature_player.state.PlayerEvents
import com.eva.player.domain.model.PlayerMetaData
import com.eva.player.domain.model.PlayerTrackData
import com.eva.player_shared.composables.PlayerDurationText
import com.eva.player_shared.util.PlayerGraphData
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.ui.theme.DownloadableFonts
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioPlayerScreenContent(
	fileModel: AudioFileModel,
	waveforms: PlayerGraphData,
	bookMarkState: CreateBookmarkState,
	trackData: () -> PlayerTrackData,
	playerMetaData: PlayerMetaData,
	bookmarks: ImmutableList<AudioBookmarkModel>,
	onPlayerEvents: (PlayerEvents) -> Unit,
	modifier: Modifier = Modifier,
	onBookmarkEvent: (BookMarkEvents) -> Unit = {},
	isControllerReady: Boolean = false,
) {
	val bookMarkTimeStamps by remember(bookmarks) {
		derivedStateOf {
			bookmarks.map(AudioBookmarkModel::timeStamp)
				.toImmutableList()
		}
	}

	Box(
		modifier = modifier.fillMaxSize()
	) {
		PlayerDurationText(
			track = trackData,
			fontFamily = DownloadableFonts.SPLINE_SANS_MONO_FONT_FAMILY,
			modifier = Modifier.align(Alignment.TopCenter),
		)
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.Center)
				.offset(y = (-80).dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(4.dp),
		) {
			PlayerAmplitudeGraph(
				trackData = trackData,
				bookMarksTimeStamps = bookMarkTimeStamps,
				graphData = waveforms,
				timelineFontFamily = DownloadableFonts.PLUS_CODE_LATIN_FONT_FAMILY,
				modifier = Modifier.fillMaxWidth()
			)
			PlayerBookMarks(
				trackData = trackData,
				bookmarks = bookmarks,
				bookMarkState = bookMarkState,
				onBookmarkEvent = onBookmarkEvent,
				modifier = Modifier.fillMaxWidth()
			)
		}
		PlayerActionsAndSlider(
			metaData = playerMetaData,
			trackData = trackData,
			isControllerSet = isControllerReady,
			onPlayerAction = onPlayerEvents,
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.BottomCenter),
		)
	}
}
