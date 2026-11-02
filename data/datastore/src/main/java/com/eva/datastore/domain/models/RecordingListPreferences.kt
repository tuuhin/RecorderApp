package com.eva.datastore.domain.models

import com.eva.datastore.domain.enums.RecordingSortOrder

data class RecordingListPreferences(
	val isAsc: Boolean = false,
	val sortOrder: RecordingSortOrder = RecordingSortOrder.DATE_CREATED
)