package com.eva.recorderapp.voice_recorder.data.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderFileSettings
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderFileSettingsRepo
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.io.OutputStream

class RecorderFileSettingsRepoImpl(
	private val context: Context
) : RecorderFileSettingsRepo {

	override val fileSettingsFlow: Flow<RecorderFileSettings>
		get() = context.recorderFileSettings.data.map(FileSettingsProto::toDomain)

	override val fileSettings: RecorderFileSettings
		get() = runBlocking { fileSettingsFlow.first() }
}


private val Context.recorderFileSettings: DataStore<FileSettingsProto> by dataStore(
	fileName = DataStoreConstants.RECORDER_FILE_SETTINGS_FILE_NAME,
	serializer = object : Serializer<FileSettingsProto> {

		override val defaultValue: FileSettingsProto = fileSettingsProto {
			namingStyle = "Voice"
			format = NamingFormatProto.FORMAAT_VIA_DATE
		}

		override suspend fun readFrom(input: InputStream): FileSettingsProto {
			try {
				return FileSettingsProto.parseFrom(input)
			} catch (exception: InvalidProtocolBufferException) {
				throw CorruptionException("Cannot read .proto file", exception)
			}
		}

		override suspend fun writeTo(t: FileSettingsProto, output: OutputStream) =
			t.writeTo(output)
	}
)