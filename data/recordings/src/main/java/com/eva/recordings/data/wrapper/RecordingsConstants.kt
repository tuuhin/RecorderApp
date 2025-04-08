package com.eva.recordings.data.wrapper

import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

internal object RecordingsConstants {

	val AUDIO_VOLUME_URI: Uri
		get() = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

	// DON'T CHANGE
	val RECORDINGS_MUSIC_PATH: String
		get() {
			// keep the recordings in recordings directory on API 31
			// otherwise music directory
			val directory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
				Environment.DIRECTORY_RECORDINGS
			else Environment.DIRECTORY_MUSIC

			return directory + File.separator + "RecorderApp"
		}
}