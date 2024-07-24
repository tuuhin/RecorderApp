package com.eva.recorderapp.voice_recorder.data.database

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@ProvidedTypeConverter
class LocalDateTimeConvertors() {

	private val timeZone: TimeZone
		get() = TimeZone.currentSystemDefault()

	@TypeConverter
	fun fromLocalDateTime(dateTime: LocalDateTime): Long {
		return dateTime.toInstant(timeZone)
			.toEpochMilliseconds()
	}

	@TypeConverter
	fun toLocalDateTime(seconds: Long): LocalDateTime {
		return Instant.fromEpochMilliseconds(seconds)
			.toLocalDateTime(timeZone)
	}

}