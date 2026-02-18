package com.eva.datastore.data.repository

import androidx.datastore.core.DataStore
import com.eva.datastore.data.mappers.toDomain
import com.eva.datastore.domain.models.TranscriptionSettings
import com.eva.datastore.domain.repository.TranscriptionSettingsRepo
import com.eva.datastore.proto.TranscriptionSettingsProto
import com.google.protobuf.StringValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class TranscriptionSettingsRepoImpl(
	private val dataStore: DataStore<TranscriptionSettingsProto>
) : TranscriptionSettingsRepo {

	override val settingsFlow: Flow<TranscriptionSettings>
		get() = dataStore.data.map(TranscriptionSettingsProto::toDomain)

	override suspend fun setting(): TranscriptionSettings {
		return withContext(Dispatchers.IO) {
			settingsFlow.first()
		}
	}

	override suspend fun onEnableOrDisable(isEnabled: Boolean) {
		dataStore.updateData { data ->
			data.toBuilder()
				.setIsEnabled(isEnabled)
				.build()
		}
	}

	override suspend fun onUpdateModelId(modelId: String?) {

		val stringValue = modelId?.let { StringValue.of(it) }
			?: StringValue.newBuilder().clearValue().build()

		dataStore.updateData { data ->
			data.toBuilder()
				.setTranscriptionModelId(stringValue)
				.build()
		}
	}
}