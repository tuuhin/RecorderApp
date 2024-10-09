package com.eva.recorderapp.voice_recorder.widgets.recorder

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.dataStoreFile
import androidx.glance.state.GlanceStateDefinition
import com.eva.recorderapp.voice_recorder.widgets.data.RecorderWidgetDataProto
import com.eva.recorderapp.voice_recorder.widgets.data.RecordingModeProto
import com.eva.recorderapp.voice_recorder.widgets.data.recorderWidgetDataProto
import com.google.protobuf.Duration
import com.google.protobuf.InvalidProtocolBufferException
import java.io.File
import java.io.InputStream
import java.io.OutputStream


object RecorderWidgetDefinition : GlanceStateDefinition<RecorderWidgetDataProto> {

	const val FILE_LOCATION = "recorder_data.proto"
	val ZERO_DURATION: Duration = Duration.newBuilder()
		.setSeconds(0).build()

	private val Context.recorderData by dataStore(FILE_LOCATION, RecorderDatastoreData)

	override suspend fun getDataStore(
		context: Context,
		fileKey: String,
	): DataStore<RecorderWidgetDataProto> = context.recorderData

	override fun getLocation(context: Context, fileKey: String): File {
		return context.dataStoreFile(FILE_LOCATION)
	}

	private object RecorderDatastoreData : Serializer<RecorderWidgetDataProto> {

		override val defaultValue: RecorderWidgetDataProto = recorderWidgetDataProto {
			mode = RecordingModeProto.IDLE_OR_COMPLETED
			duration = ZERO_DURATION
		}

		override suspend fun readFrom(input: InputStream): RecorderWidgetDataProto {
			try {
				return RecorderWidgetDataProto.parseFrom(input)
			} catch (exception: InvalidProtocolBufferException) {
				throw CorruptionException("Cannot read .proto file", exception)
			}
		}

		override suspend fun writeTo(t: RecorderWidgetDataProto, output: OutputStream) =
			t.writeTo(output)
	}

}