package com.eva.database.convertors

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import kotlinx.datetime.LocalTime

@ProvidedTypeConverter
internal class LocalTimeConvertors {

	@TypeConverter
	fun fromLocalDateTime(dateTime: LocalTime) = dateTime.toMillisecondOfDay()


	@TypeConverter
	fun toLocalDateTime(seconds: Int) = LocalTime.fromMillisecondOfDay(seconds)

}