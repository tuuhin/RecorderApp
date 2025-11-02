package com.eva.datastore.data.repository

import androidx.datastore.core.DataStore
import com.eva.datastore.data.mappers.toDomain
import com.eva.datastore.data.mappers.toProto
import com.eva.datastore.domain.enums.RecordQuality
import com.eva.datastore.domain.enums.RecordingEncoders
import com.eva.datastore.domain.models.RecorderAudioSettings
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.datastore.proto.RecorderSettingsProto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class RecorderAudioSettingsRepoImpl(
	private val datastore: DataStore<RecorderSettingsProto>
) : RecorderAudioSettingsRepo {

	override val audioSettingsFlow: Flow<RecorderAudioSettings>
		// its will only emit values if the values are distinct
		get() = datastore.data.map(RecorderSettingsProto::toDomain)

	override suspend fun audioSettings(): RecorderAudioSettings {
		return withContext(Dispatchers.IO) { audioSettingsFlow.first() }
	}

	override suspend fun onEncoderChange(encoder: RecordingEncoders) {
		datastore.updateData { settings ->
			settings.toBuilder()
				.setEncoder(encoder.toProto)
				.build()
		}
	}

	override suspend fun onQualityChange(quality: RecordQuality) {
		datastore.updateData { settings ->
			settings.toBuilder()
				.setQuality(quality.toProto)
				.build()
		}
	}

	override suspend fun onStereoModeChange(mode: Boolean) {
		datastore.updateData { settings ->
			settings.toBuilder()
				.setIsStereoMode(mode)
				.build()
		}
	}

	override suspend fun onSkipSilencesChange(skipAllowed: Boolean) {
		datastore.updateData { settings ->
			settings.toBuilder()
				.setSkipSilences(skipAllowed)
				.build()
		}
	}

	override suspend fun onUseBluetoothMicEnabled(isAllowed: Boolean) {
		datastore.updateData { settings ->
			settings.toBuilder()
				.setUseBluetoothMic(isAllowed)
				.build()
		}
	}

	override suspend fun onPauseRecorderOnCallEnabled(isEnabled: Boolean) {
		datastore.updateData { settings ->
			settings.toBuilder()
				.setPauseDuringCalls(isEnabled)
				.build()
		}
	}

	override suspend fun onAddLocationEnabled(isEnabled: Boolean) {
		datastore.updateData { settings ->
			settings.toBuilder()
				.setAllowLocationInfoIfAvailable(isEnabled)
				.build()
		}
	}
}