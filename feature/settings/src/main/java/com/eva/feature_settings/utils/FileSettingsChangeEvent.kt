package com.eva.feature_settings.utils

import com.eva.datastore.domain.enums.AudioFileNamingFormat

internal sealed interface FileSettingsChangeEvent {

	data class OnFormatChange(val format: AudioFileNamingFormat) : FileSettingsChangeEvent

	data class OnRecordingPrefixChange(val prefix: String) : FileSettingsChangeEvent

	data class OnExportItemPrefixChange(val prefix: String) : FileSettingsChangeEvent

	data class OnAllowExternalFiles(val isEnabled: Boolean) : FileSettingsChangeEvent
}