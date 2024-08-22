package com.eva.recorderapp.voice_recorder.data.player

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioFileModel

internal fun AudioFileModel.toMediaItem(): MediaItem {
	// adding much of the metadata available from audiofile
	val metaData = MediaMetadata.Builder()
		.setTitle(title)
		.setDisplayTitle(displayName)
		.setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
		.setRecordingDay(lastModified.dayOfMonth)
		.setRecordingMonth(lastModified.monthNumber)
		.setRecordingYear(lastModified.year)
		.setIsBrowsable(false)
		.build()

	val fileUri = fileUri.toUri()
	return MediaItem.Builder()
		.setUri(fileUri)
		.setMediaId("${id}")
		.setMimeType(mimeType)
		.setMediaMetadata(metaData)
		.build()
}