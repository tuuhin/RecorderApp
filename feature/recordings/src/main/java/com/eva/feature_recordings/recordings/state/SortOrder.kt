package com.eva.feature_recordings.recordings.state

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eva.ui.R

internal enum class SortOrder {
	ASC,
	DESC;

	val strResource: String
		@Composable
		get() = when (this) {
			ASC -> stringResource(R.string.sort_order_asc)
			DESC -> stringResource(R.string.sort_order_desc)
		}
}