package com.eva.recorderapp.voice_recorder.presentation.recordings.util.state

import androidx.annotation.StringRes
import com.eva.recorderapp.R

enum class SortOrder(@StringRes val res: Int) {
	ASC(R.string.sort_order_asc),
	DESC(R.string.sort_order_desc)
}