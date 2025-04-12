package com.eva.datastore.data.repository

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.eva.datastore.data.DataStoreConstants
import com.eva.datastore.data.mappers.toDomain
import com.eva.datastore.data.mappers.toProto
import com.eva.datastore.domain.enums.RecordQuality
import com.eva.datastore.domain.enums.RecordingEncoders
import com.eva.datastore.domain.models.RecorderAudioSettings
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.datastore.proto.RecorderSettingsProto
import com.eva.datastore.proto.recorderSettingsProto
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.io.OutputStream

internal class RecorderAudioSettingsRepoImpl(private val context: Context) :
	RecorderAudioSettingsRepo {

	override val audioSettingsFlow: Flow<RecorderAudioSettings>
		get() = context.recorderSettings.data.map(RecorderSettingsProto::toDomain)

	override val audioSettings: RecorderAudioSettings
		get() = runBlocking { audioSettingsFlow.first() }

	override suspend fun onEncoderChange(encoder: RecordingEncoders) {
		context.recorderSettings.updateData { settings ->
			settings.toBuilder()
				.setEncoder(encoder.toProto)
				.build()
		}
	}

	override suspend fun onQualityChange(quality: RecordQuality) {
		context.recorderSettings.updateData { settings ->
			settings.toBuilder()
				.setQuality(quality.toProto)
				.build()
		}
	}

	override suspend fun onStereoModeChange(mode: Boolean) {
		context.recorderSettings.updateData { settings ->
			settings.toBuilder()
				.setIsStereoMode(mode)
				.build()
		}
	}

	override suspend fun onSkipSilencesChange(skipAllowed: Boolean) {
		context.recorderSettings.updateData { settings ->
			settings.toBuilder()
				.setSkipSilences(skipAllowed)
				.build()
		}
	}

	override suspend fun onUseBluetoothMicEnabled(isAllowed: Boolean) {
		context.recorderSettings.updateData { settings ->
			settings.toBuilder()
				.setUseBluetoothMic(isAllowed)
				.build()
		}
	}

	override suspend fun onPauseRecorderOnCallEnabled(isEnabled: Boolean) {
		context.recorderSettings.updateData { settings ->
			settings.toBuilder()
				.setPauseDuringCalls(isEnabled)
				.build()
		}
	}

	override suspend fun onAddLocationEnabled(isEnabled: Boolean) {
		context.recorderSettings.updateData { settings ->
			settings.toBuilder()
				.setAllowLocationInfoIfAvailable(isEnabled)
				.build()
		}
	}
}

private val Context.recorderSettings: DataStore<RecorderSettingsProto> by dataStore(
	fileName = DataStoreConstants.RECORDER_SETTINGS_FILE_NAME,
	serializer = object : Serializer<RecorderSettingsProto> {

		override val defaultValue: RecorderSettingsProto = recorderSettingsProto {}

		override suspend fun readFrom(input: InputStream): RecorderSettingsProto {
			try {
				return RecorderSettingsProto.parseFrom(input)
			} catch (exception: InvalidProtocolBufferException) {
				throw CorruptionException("Cannot read .proto file", exception)
			}
		}

		override suspend fun writeTo(t: RecorderSettingsProto, output: OutputStream) =
			t.writeTo(output)
	}
)