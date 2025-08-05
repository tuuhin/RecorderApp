package com.eva.database.convertors

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@ProvidedTypeConverter
internal class LocalDateTimeConvertors {

	private val timeZone: TimeZone
		get() = TimeZone.currentSystemDefault()

	@TypeConverter
	fun fromLocalDateTime(dateTime: LocalDateTime): Long = dateTime.toInstant(timeZone)
		.toEpochMilliseconds()


	@TypeConverter
	fun toLocalDateTime(seconds: Long) =
		Instant.fromEpochMilliseconds(seconds).toLocalDateTime(timeZone)


}