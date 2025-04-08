package com.eva.feature_widget.recorder.repository

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.eva.feature_widget.proto.RecorderWidgetDataProto
import com.eva.feature_widget.proto.RecordingModeProto
import com.eva.feature_widget.proto.recorderWidgetDataProto
import com.eva.recorder.domain.models.RecorderState
import com.google.protobuf.Duration
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime
import java.io.InputStream
import java.io.OutputStream

class RecorderWidgetRepoImpl(private val context: Context) : RecorderWidgetRepo {

	override suspend fun updateRecordingWidget(state: RecorderState, time: LocalTime?) {
		val duration = Duration.newBuilder()
			.setSeconds(time?.second?.toLong() ?: 0L).build()

		val recordingMode = when (state) {
			RecorderState.RECORDING -> RecordingModeProto.RECORDING
			RecorderState.PAUSED -> RecordingModeProto.PAUSED
			else -> RecordingModeProto.IDLE_OR_COMPLETED
		}

		withContext(Dispatchers.IO) {
			// update the content
			context.recorderWidgetData.updateData { content ->
				content.toBuilder()
					.setMode(recordingMode)
					.setDuration(duration)
					.build()
			}
		}
	}

	override suspend fun resetRecorderWidget() {
		withContext(Dispatchers.IO) {
			// update the content
			context.recorderWidgetData.updateData { content ->
				content.toBuilder()
					.setMode(RecordingModeProto.IDLE_OR_COMPLETED)
					.setDuration(ZERO_DURATION)
					.build()
			}
		}
	}

	private object RecorderWidgetInfoSerializer : Serializer<RecorderWidgetDataProto> {

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

	companion object {
		const val FILE_LOCATION = "recorder_data.proto"

		private val ZERO_DURATION: Duration = Duration.newBuilder()
			.setSeconds(0).build()

		val Context.recorderWidgetData by dataStore(FILE_LOCATION, RecorderWidgetInfoSerializer)
	}
}