package com.eva.datastore.data.serializers

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.eva.datastore.proto.RecordingListSettingsProto
import com.eva.datastore.proto.SortingOrderProto
import com.eva.datastore.proto.recordingListSettingsProto
import java.io.InputStream
import java.io.OutputStream

internal object RecordingsListOrderSerializer : Serializer<RecordingListSettingsProto> {

	override val defaultValue: RecordingListSettingsProto
		get() = recordingListSettingsProto {
			isAsc = true
			order = SortingOrderProto.DATE_CREATED
		}

	override suspend fun readFrom(input: InputStream): RecordingListSettingsProto {
		return try {
			RecordingListSettingsProto.parseFrom(input)
		} catch (e: CorruptionException) {
			throw e
		}
	}

	override suspend fun writeTo(t: RecordingListSettingsProto, output: OutputStream) =
		t.writeTo(output)

}