package com.eva.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

object LocalTimeFormats {

	val LOCAL_DATETIME_DATE_TIME_FORMAT = LocalDateTime.Format {
		day(padding = Padding.ZERO)
		char(' ')
		monthName(MonthNames.ENGLISH_FULL)
		char(' ')
		year()
		char(' ')
		amPmHour()
		char(':')
		minute()
		char(' ')
		amPmMarker("am", "pm")
	}

	val NOTIFICATION_TIMER_TIME_FORMAT = LocalTime.Format {
		hour()
		char(':')
		minute()
		char(':')
		second()
	}

	val LOCALTIME_FORMAT_HH_MM_SS_SF2 = LocalTime.Format {
		hour()
		char(':')
		minute()
		char(':')
		second()
		char('.')
		secondFraction(2)
	}


	val LOCALTIME_FORMAT_MM_SS_SF2 = LocalTime.Format {
		minute()
		char(':')
		second()
		char('.')
		secondFraction(2)
	}

	val LOCALTIME_FORMAT_MM_SS = LocalTime.Format {
		minute()
		char(':')
		second()
	}

	val LOCALTIME_FORMAT_HH_MM_SS = LocalTime.Format {
		hour()
		char(':')
		minute()
		char(':')
		second()
	}

	val RECORDING_RECORD_TIME_FORMAT = LocalDateTime.Format {
		day(padding = Padding.ZERO)
		char(' ')
		monthName(MonthNames.ENGLISH_ABBREVIATED)
		char(' ')
		hour(padding = Padding.NONE)
		char(':')
		minute()
		char(' ')
	}

	val LOCALTIME_HH_MM_SS_FORMAT = LocalTime.Format {
		hour()
		char(':')
		minute()
		char(':')
		second()
	}
}