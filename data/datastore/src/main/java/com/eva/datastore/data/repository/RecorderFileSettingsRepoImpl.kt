package com.eva.datastore.data.repository

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.eva.datastore.data.DataStoreConstants
import com.eva.datastore.data.mappers.toDomain
import com.eva.datastore.data.mappers.toProto
import com.eva.datastore.domain.enums.AudioFileNamingFormat
import com.eva.datastore.domain.models.RecorderFileSettings
import com.eva.datastore.domain.repository.RecorderFileSettingsRepo
import com.eva.datastore.proto.FileSettingsProto
import com.eva.datastore.proto.NamingFormatProto
import com.eva.datastore.proto.fileSettingsProto
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.io.OutputStream

internal class RecorderFileSettingsRepoImpl(private val context: Context) :
	RecorderFileSettingsRepo {

	override val fileSettingsFlow: Flow<RecorderFileSettings>
		get() = context.recorderFileSettings.data.map(FileSettingsProto::toDomain)

	override val fileSettings: RecorderFileSettings
		get() = runBlocking { fileSettingsFlow.first() }

	override suspend fun onFileNameFormatChange(format: AudioFileNamingFormat) {
		context.recorderFileSettings.updateData { settings ->
			settings.toBuilder()
				.setFormat(format.toProto)
				.build()
		}
	}

	override suspend fun onFilePrefixChange(prefix: String) {
		context.recorderFileSettings.updateData { settings ->
			settings.toBuilder()
				.setPrefix(prefix)
				.build()
		}
	}

	override suspend fun onAllowExternalFileRead(isAllowed: Boolean) {
		context.recorderFileSettings.updateData { settings ->
			settings.toBuilder()
				.setAllowExternalRead(isAllowed)
				.build()
		}
	}

	override suspend fun onExportItemPrefixChange(prefix: String) {
		context.recorderFileSettings.updateData { settings ->
			settings.toBuilder()
				.setExportedItemPrefix(prefix)
				.build()
		}
	}
}

private val Context.recorderFileSettings: DataStore<FileSettingsProto> by dataStore(
	fileName = DataStoreConstants.RECORDER_FILE_SETTINGS_FILE_NAME,
	serializer = object : Serializer<FileSettingsProto> {

		override val defaultValue: FileSettingsProto = fileSettingsProto {
			prefix = RecorderFileSettings.NORMAL_FILE_PREFIX
			exportedItemPrefix = RecorderFileSettings.EXPORT_FILE_PREFIX
			format = NamingFormatProto.FORMAT_VIA_DATE
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