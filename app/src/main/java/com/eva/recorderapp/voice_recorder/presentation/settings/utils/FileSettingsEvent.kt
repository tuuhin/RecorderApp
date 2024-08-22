package com.eva.recorderapp.voice_recorder.presentation.settings.utils

import com.eva.recorderapp.voice_recorder.domain.datastore.enums.AudioFileNamingFormat

sealed interface FileSettingsEvent {

	data class OnFormatChange(val format: AudioFileNamingFormat) : FileSettingsEvent
}