package com.eva.datastore.data.serializers

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.eva.datastore.proto.TranscriptionSettingsProto
import com.eva.datastore.proto.transcriptionSettingsProto
import com.google.protobuf.StringValue
import java.io.InputStream
import java.io.OutputStream

internal object TranscriptionSettingsSerializer : Serializer<TranscriptionSettingsProto> {

	override val defaultValue: TranscriptionSettingsProto
		get() = transcriptionSettingsProto {
			isEnabled = false
			transcriptionModelId = StringValue.getDefaultInstance().toBuilder()
				.clearValue()
				.build()
		}

	override suspend fun readFrom(input: InputStream): TranscriptionSettingsProto {
		return try {
			TranscriptionSettingsProto.parseFrom(input)
		} catch (e: CorruptionException) {
			throw e
		}
	}

	override suspend fun writeTo(t: TranscriptionSettingsProto, output: OutputStream) =
		t.writeTo(output)
}