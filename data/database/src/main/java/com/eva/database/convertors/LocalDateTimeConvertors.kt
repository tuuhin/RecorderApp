package com.eva.database.convertors

import androidx.room3.ColumnTypeConverter
import androidx.room3.ColumnTypeConverters
import androidx.room3.ProvidedColumnTypeConverter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@ProvidedColumnTypeConverter
internal class LocalDateTimeConvertors {

	private val timeZone: TimeZone
		get() = TimeZone.UTC

	@ColumnTypeConverter
	fun fromLocalDateTime(dateTime: LocalDateTime): Long = dateTime.toInstant(timeZone)
		.toEpochMilliseconds()


	@ColumnTypeConverter
	fun toLocalDateTime(milliSeconds: Long) =
		Instant.fromEpochMilliseconds(milliSeconds).toLocalDateTime(timeZone)

}