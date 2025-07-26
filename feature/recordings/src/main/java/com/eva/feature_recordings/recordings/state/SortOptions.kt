package com.eva.feature_recordings.recordings.state

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eva.ui.R

internal enum class SortOptions {
	DATE_CREATED,
	DURATION,
	NAME,
	SIZE;

	val strRes: String
		@Composable
		get() = when (this) {
			DATE_CREATED -> stringResource(R.string.sort_option_date_created)
			DURATION -> stringResource(R.string.sort_option_duration)
			NAME -> stringResource(R.string.sort_option_name)
			SIZE -> stringResource(R.string.sort_option_size)
		}
}