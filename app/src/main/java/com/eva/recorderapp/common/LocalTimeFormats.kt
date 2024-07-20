package com.eva.recorderapp.common

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

object LocalTimeFormats {

	val NOTIFICATION_TIMER_TIME_FORMAT = LocalTime.Format {
		hour()
		char(':')
		minute()
		char(':')
		second()
	}

	val PRESENTATON_TIMER_TIME_FORMAT = LocalTime.Format {
		hour()
		char(':')
		minute()
		char(':')
		second()
		char('.')
		secondFraction(2)
	}

	val RECORDING_RECORD_TIME_FORMAT = LocalDateTime.Format {
		dayOfMonth()
		char(' ')
		monthName(MonthNames.ENGLISH_ABBREVIATED)
		char(' ')
		hour(padding = Padding.NONE)
		char(':')
		minute()
		char(' ')
		amPmMarker("am", "pm")
	}
}