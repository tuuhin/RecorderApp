package com.eva.recorderapp.voice_recorder.presentation.recordings.search

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eva.recorderapp.R

enum class SearchFilterTimeOption(@StringRes private val stringResource: Int) {
	TODAY(R.string.time_filter_today),
	YESTERDAY(R.string.time_filter_yesterday),
	WEEK(R.string.time_filter_week),
	THIS_MONTH(R.string.time_filter_this_month),
	LAST_MONTH(R.string.time_filter_last_month),
	THIS_YEAR(R.string.time_filter_this_year);

	val strRes: String
		@Composable
		get() = stringResource(id = stringResource)
}