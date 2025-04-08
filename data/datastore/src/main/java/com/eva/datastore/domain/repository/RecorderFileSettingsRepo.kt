package com.eva.datastore.domain.repository

import com.eva.datastore.domain.enums.AudioFileNamingFormat
import com.eva.datastore.domain.models.RecorderFileSettings
import kotlinx.coroutines.flow.Flow

interface RecorderFileSettingsRepo {

	val fileSettingsFlow: Flow<RecorderFileSettings>

	val fileSettings: RecorderFileSettings

	suspend fun onFilePrefixChange(prefix: String)

	suspend fun onFileNameFormatChange(format: AudioFileNamingFormat)

	suspend fun onAllowExternalFileRead(isAllowed: Boolean)
}