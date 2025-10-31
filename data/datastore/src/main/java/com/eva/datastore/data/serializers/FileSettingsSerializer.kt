package com.eva.datastore.data.serializers

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.eva.datastore.domain.models.RecorderFileSettings
import com.eva.datastore.proto.FileSettingsProto
import com.eva.datastore.proto.NamingFormatProto
import com.eva.datastore.proto.fileSettingsProto
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

internal object FileSettingsSerializer : Serializer<FileSettingsProto> {

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