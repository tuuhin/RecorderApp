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
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioFileModel
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.datetime.format

@Composable
fun AudioFileMetaDataSheetContent(
	audio: AudioFileModel,
	modifier: Modifier = Modifier,
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
		FileMetaData(
			title = stringResource(id = R.string.audio_metadata_name_title),
			text = audio.displayName
		)
		FileMetaData(
			title = stringResource(id = R.string.audio_metadata_file_size_title),
			text = fileSize
		)
		FileMetaData(
			title = stringResource(id = R.string.audio_metadata_duration_title),
			text = durationText
		)
		FileMetaData(
			title = stringResource(id = R.string.audio_metadata_mime_type_title),
			text = audio.mimeType
		)
		FileMetaData(
			title = stringResource(id = R.string.audio_metadata_last_edit_title),
			text = lastModified
		)
		FileMetaData(
			title = stringResource(id = R.string.audio_metadata_channel_title),
			text = stringResource(
				id = if (audio.channel == 1) R.string.audio_metadata_channel_mono
				else R.string.audio_metadata_channel_stereo
			)
		)
		FileMetaData(
			title = stringResource(id = R.string.audio_metadata_bitrate_title),
			text = stringResource(id = R.string.audio_metadata_bitrate, audio.bitRateInKbps)
		)
		FileMetaData(
			title = stringResource(id = R.string.audio_metadata_sample_rate_title),
			text = stringResource(id = R.string.audio_metadata_sample_rate, audio.samplingRateKHz)
		)
		FileMetaData(
			title = stringResource(id = R.string.audio_metadata_path_title),
			text = audio.path
		)
		if (audio.hasLocation) {
			FileMetaData(
				title = stringResource(R.string.audio_metadata_file_location),
				text = audio.metaDataLocation
			)
		}
	}
}

@Composable
private fun FileMetaData(
	title: String,
	text: String,
	modifier: Modifier = Modifier,
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