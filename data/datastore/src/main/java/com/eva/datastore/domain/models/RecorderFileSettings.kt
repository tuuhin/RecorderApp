package com.eva.datastore.domain.models

import com.eva.datastore.domain.enums.AudioFileNamingFormat

data class RecorderFileSettings(
	val name: String = NORMAL_FILE_PREFIX,
	val exportItemPrefix: String = EXPORT_FILE_PREFIX,
	val format: AudioFileNamingFormat = AudioFileNamingFormat.DATE_TIME,
	val allowExternalRead: Boolean = false,
) {
	companion object {
		const val NORMAL_FILE_PREFIX = "Voice"
		const val EXPORT_FILE_PREFIX = "Exported"
	}
}
