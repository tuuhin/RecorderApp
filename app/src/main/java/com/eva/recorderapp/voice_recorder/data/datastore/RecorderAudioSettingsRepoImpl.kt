package com.eva.recorderapp.voice_recorder.data.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordQuality
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.RecordingEncoders
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderAudioSettings
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderAudioSettingsRepo
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.io.OutputStream

class RecorderAudioSettingsRepoImpl(
	private val context: Context
) : RecorderAudioSettingsRepo {

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