package com.eva.recorderapp.voice_recorder.presentation.recordings.util.state

import androidx.annotation.StringRes
import com.eva.recorderapp.R

enum class SortOptions(@StringRes val res:Int) {
	DATE_CREATED(R.string.sort_option_date_created),
	DURATION(R.string.sort_option_duration),
	NAME(R.string.sort_option_name)
}