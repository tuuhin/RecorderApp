package com.eva.recorderapp.common

import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional

val NOTIFICATION_TIMER_TIME_FORMAT = LocalTime.Format {
	optional {
		hour()
		char(':')
	}
	minute()
	char(':')
	second()
	char('.')
	secondFraction(1)
}