package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.common.LocalTimeFormats
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.models.AudioFileModel
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.datetime.format

@Composable
fun AudioFileMetaDataSheetContent(
	audio: AudioFileModel,
	modifier: Modifier = Modifier
) {

	val context = LocalContext.current

	val fileSize = remember(audio.size) {
		Formatter.formatFileSize(context, audio.size)
	}

	val durationText = remember(audio.duration) {
		audio.durationAsLocaltime.format(LocalTimeFormats.LOCALTIME_HH_MM_SS_FORMAT)
	}

	val lastModified = remember(audio.lastModified) {
		audio.lastModified.format(LocalTimeFormats.LOCALDATETIME_DATE_TIME_FORMAT)
	}

	val bitRate = remember(audio.bitRateInKbps) {
		"${audio.bitRateInKbps}kbps"
	}

	val samplingRate = remember(audio.samplingRatekHz) {
		"${audio.samplingRatekHz}Khz"
	}

	Column(
		modifier = modifier.padding(12.dp),
		verticalArrangement = Arrangement.spacedBy(4.dp)
	) {
		Text(
			text = stringResource(id = R.string.meta_data_sheet_heading),
			style = MaterialTheme.typography.titleLarge,
			color = MaterialTheme.colorScheme.onSurface
		)
		Spacer(modifier = Modifier.height(4.dp))
		FileMetaData(title = "Name", text = audio.displayName)
		FileMetaData(title = "Size", text = fileSize)
		FileMetaData(title = "Duration", text = durationText)
		FileMetaData(title = "Last Modified", text = lastModified)
		FileMetaData(title = "Channel", text = "Mono ${audio.channel}")
		FileMetaData(title = "Bit Rate", text = bitRate)
		FileMetaData(title = "Sampling Rate", text = samplingRate)
		FileMetaData(title = "Path", text = audio.path)
	}
}

@Composable
private fun FileMetaData(
	title: String,
	text: String,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier.padding(vertical = 8.dp, horizontal = 4.dp)
	) {
		Text(
			text = title,
			style = MaterialTheme.typography.labelLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Text(
			text = text,
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onBackground
		)
	}
}

@PreviewLightDark
@Composable
private fun AudioFileMetaDataPreview() = RecorderAppTheme {
	Surface(color = MaterialTheme.colorScheme.background) {
		AudioFileMetaDataSheetContent(
			audio = PreviewFakes.FAKE_AUDIO_MODEL,
			modifier = Modifier.fillMaxWidth()
		)
	}
}