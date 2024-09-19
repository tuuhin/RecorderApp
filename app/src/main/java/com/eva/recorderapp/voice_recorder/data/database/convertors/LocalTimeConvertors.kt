package com.eva.recorderapp.voice_recorder.data.database.convertors

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import kotlinx.datetime.LocalTime

@ProvidedTypeConverter
class LocalTimeConvertors {

	@TypeConverter
	fun fromLocalDateTime(dateTime: LocalTime): Int {
		return dateTime.toMillisecondOfDay()
	}

	@TypeConverter
	fun toLocalDateTime(seconds: Int): LocalTime {
		return LocalTime.fromMillisecondOfDay(seconds)
	}
}