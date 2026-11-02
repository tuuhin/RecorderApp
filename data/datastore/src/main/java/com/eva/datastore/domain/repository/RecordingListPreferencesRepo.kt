package com.eva.datastore.domain.repository

import com.eva.datastore.domain.enums.RecordingSortOrder
import com.eva.datastore.domain.models.RecordingListPreferences
import kotlinx.coroutines.flow.Flow

interface RecordingListPreferencesRepo {

	val listPreferencesOrder: Flow<RecordingListPreferences>

	suspend fun listPreferences(): RecordingListPreferences

	suspend fun setIsAscending(isEnabled: Boolean)

	suspend fun setSortOrder(order: RecordingSortOrder)
}