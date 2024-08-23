package com.eva.recorderapp.voice_recorder.presentation.settings.utils

import com.eva.recorderapp.voice_recorder.domain.datastore.enums.AudioFileNamingFormat

sealed interface FileSettingsChangeEvent {

	data class OnFormatChange(val format: AudioFileNamingFormat) : FileSettingsChangeEvent

	data class OnRecordingPrefixChange(val prefix: String) : FileSettingsChangeEvent
}