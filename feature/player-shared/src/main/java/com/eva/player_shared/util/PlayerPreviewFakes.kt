package com.eva.player_shared.util

import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object PlayerPreviewFakes {

	private val GRAPH_DATA = listOf(
		0.123f,
		0.456f,
		0.789f,
		0.234f,
		0.567f,
		0.890f,
		0.345f,
		0.678f,
		0.901f,
		0.012f,
		0.345f,
		0.678f,
		0.901f,
		0.123f,
		0.456f,
		0.789f,
		0.234f,
		0.567f,
		0.890f,
		0.012f,
		0.345f,
		0.678f,
		0.901f,
		0.123f,
		0.456f,
		0.789f,
		0.234f,
		0.567f,
		0.890f,
		0.012f,
		0.345f,
		0.678f,
		0.901f,
		0.123f,
		0.456f,
		0.789f,
		0.234f,
		0.567f,
		0.890f,
		0.012f,
		0.345f,
		0.678f,
		0.901f,
		0.123f,
		0.456f,
		0.789f,
		0.234f,
		0.567f,
		0.890f,
		0.012f,
		0.345f,
		0.678f,
		0.901f,
		0.123f,
		0.456f,
		0.789f,
		0.234f,
		0.567f,
		0.890f,
		0.012f,
		0.345f,
		0.678f,
		0.901f,
		0.123f,
		0.456f,
		0.789f,
		0.234f,
		0.567f,
		0.890f,
		0.012f,
		0.345f,
		0.678f,
		0.901f,
		0.123f,
		0.456f,
		0.789f,
		0.234f,
		0.567f,
		0.890f,
		0.012f,
		0.345f,
		0.678f,
		0.901f,
		0.123f,
		0.456f,
		0.789f,
		0.234f,
		0.567f,
		0.890f,
		0.012f,
		0.345f,
		0.678f,
		0.901f,
		0.123f,
		0.456f,
		0.789f,
		0.234f,
		0.567f,
		0.890f,
		0.012f,
		0.345f,
		0.678f,
		0.901f,
		0.123f
	)

	val PREVIEW_RECORDER_AMPLITUDES = GRAPH_DATA.toFloatArray()

	fun loadAmplitudeGraph(duration: Duration = 10.seconds, base: Int = 100): FloatArray {
		val noOfPoints = duration.inWholeMilliseconds.toInt() / base
		val data = if (noOfPoints >= GRAPH_DATA.size) GRAPH_DATA
		else GRAPH_DATA.take(noOfPoints)
		return data.toFloatArray()
	}

	val FAKE_AUDIO_MODEL = AudioFileModel(
		id = 0L,
		title = "Voice_001",
		displayName = "Voice_001.abc",
		duration = 5.minutes,
		fileUri = "",
		bitRateInKbps = 0f,
		lastModified = LocalDateTime.now().toKotlinLocalDateTime(),
		samplingRateKHz = 0f,
		path = "this_is_a_path/file",
		channel = 1,
		size = 100L,
		mimeType = "This/that",
		isFavourite = true
	)

	val FAKE_TRACK_DATA = PlayerTrackData(current = 4.seconds, total = 10.seconds)
}