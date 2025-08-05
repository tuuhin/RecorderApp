package com.eva.feature_recordings.search.state

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eva.ui.R

internal enum class SearchFilterTimeOption {
	TODAY,
	YESTERDAY,
	WEEK,
	THIS_MONTH,
	LAST_MONTH,
	THIS_YEAR;

	val strRes: String
		@Composable
		get() = when (this) {
			TODAY -> stringResource(R.string.time_filter_today)
			YESTERDAY -> stringResource(R.string.time_filter_yesterday)
			WEEK -> stringResource(R.string.time_filter_week)
			THIS_MONTH -> stringResource(R.string.time_filter_this_month)
			LAST_MONTH -> stringResource(R.string.time_filter_last_month)
			THIS_YEAR -> stringResource(R.string.time_filter_this_year)
		}

}