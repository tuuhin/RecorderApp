package com.eva.datastore.data.repository

import androidx.datastore.core.DataStore
import com.eva.datastore.domain.enums.RecordingSortOrder
import com.eva.datastore.domain.models.RecordingListPreferences
import com.eva.datastore.domain.repository.RecordingListPreferencesRepo
import com.eva.datastore.proto.RecordingListSettingsProto
import com.eva.datastore.proto.SortingOrderProto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RecordingsListPreferencesRepoImpl(
	val dataStore: DataStore<RecordingListSettingsProto>
) : RecordingListPreferencesRepo {

	override val listPreferencesOrder: Flow<RecordingListPreferences>
		get() = dataStore.data.map { proto ->
			RecordingListPreferences(isAsc = proto.isAsc, sortOrder = proto.order.toModel())
		}

	override suspend fun setIsAscending(isEnabled: Boolean) {
		dataStore.updateData { settings ->
			settings.toBuilder()
				.setIsAsc(isEnabled)
				.build()
		}
	}

	override suspend fun setSortOrder(order: RecordingSortOrder) {
		val protoOrder = when (order) {
			RecordingSortOrder.DATE_CREATED -> SortingOrderProto.DATE_CREATED
			RecordingSortOrder.DURATION -> SortingOrderProto.DURATION
			RecordingSortOrder.NAME -> SortingOrderProto.NAME
			RecordingSortOrder.SIZE -> SortingOrderProto.SIZE
		}

		dataStore.updateData { settings ->
			settings.toBuilder()
				.setOrder(protoOrder)
				.build()
		}
	}

	override suspend fun listPreferences(): RecordingListPreferences {
		return listPreferencesOrder.first()
	}

	private fun SortingOrderProto.toModel() = when (this) {
		SortingOrderProto.DATE_CREATED -> RecordingSortOrder.DATE_CREATED
		SortingOrderProto.DURATION -> RecordingSortOrder.DURATION
		SortingOrderProto.NAME -> RecordingSortOrder.NAME
		SortingOrderProto.SIZE -> RecordingSortOrder.SIZE
		SortingOrderProto.UNRECOGNIZED -> RecordingSortOrder.DATE_CREATED
	}
}