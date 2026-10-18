package com.eva.database.convertors

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter
import kotlinx.datetime.LocalTime

@ProvidedColumnTypeConverter
internal class LocalTimeConvertors {

	@ColumnTypeConverter
	fun fromLocalDateTime(dateTime: LocalTime) = dateTime.toMillisecondOfDay()


	@ColumnTypeConverter
	fun toLocalDateTime(seconds: Int) = LocalTime.fromMillisecondOfDay(seconds)
}