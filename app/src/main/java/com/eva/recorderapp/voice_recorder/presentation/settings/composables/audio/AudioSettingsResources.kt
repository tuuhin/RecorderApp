package com.eva.recorderapp.voice_recorder.presentation.settings.composables.audio

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordQuality
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordingEncoders

val RecordQuality.strRes: String
	@Composable
	get() = when (this) {
		RecordQuality.HIGH -> stringResource(id = R.string.recording_settings_quality_high)
		RecordQuality.NORMAL -> stringResource(id = R.string.recording_settings_quality_normal)
		RecordQuality.LOW -> stringResource(id = R.string.recording_settings_quality_low)
	}

val RecordingEncoders.titleStrRes: String
	@Composable
	get() = when (this) {
		RecordingEncoders.ACC -> stringResource(id = R.string.recording_settings_encoder_acc)
		RecordingEncoders.AMR_WB -> stringResource(id = R.string.recording_settings_encoder_amr_wb)
		RecordingEncoders.AMR_NB -> stringResource(id = R.string.recording_settings_encoder_three_gpp)
		RecordingEncoders.OPTUS -> stringResource(id = R.string.recording_settings_encoder_optus)
	}

val RecordingEncoders.descriptionStrRes: String
	@Composable
	get() = when (this) {
		RecordingEncoders.AMR_NB -> stringResource(id = R.string.recording_settings_encoder_amr_nb_text)
		RecordingEncoders.AMR_WB -> stringResource(id = R.string.recording_settings_encoder_amr_wb_text)
		RecordingEncoders.ACC -> stringResource(id = R.string.recording_settings_encoder_mpeg_text)
		RecordingEncoders.OPTUS -> stringResource(id = R.string.recording_settings_encoder_optus_text)
	}