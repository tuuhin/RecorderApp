package com.eva.datastore.data.serializers

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.eva.datastore.proto.RecorderSettingsProto
import com.eva.datastore.proto.recorderSettingsProto
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

internal object RecorderSettingsSerializer : Serializer<RecorderSettingsProto> {

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