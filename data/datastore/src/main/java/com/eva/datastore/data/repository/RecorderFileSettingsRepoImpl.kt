package com.eva.datastore.data.repository

import androidx.datastore.core.DataStore
import com.eva.datastore.data.mappers.toDomain
import com.eva.datastore.data.mappers.toProto
import com.eva.datastore.domain.enums.AudioFileNamingFormat
import com.eva.datastore.domain.models.RecorderFileSettings
import com.eva.datastore.domain.repository.RecorderFileSettingsRepo
import com.eva.datastore.proto.FileSettingsProto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class RecorderFileSettingsRepoImpl(
	private val dataStore: DataStore<FileSettingsProto>
) : RecorderFileSettingsRepo {

	override val fileSettingsFlow: Flow<RecorderFileSettings>
		// its will only emit values if the values are distinct
		get() = dataStore.data.map(FileSettingsProto::toDomain)

	override suspend fun fileSettings(): RecorderFileSettings {
		return withContext(Dispatchers.IO) { fileSettingsFlow.first() }
	}

	override suspend fun onFileNameFormatChange(format: AudioFileNamingFormat) {
		dataStore.updateData { settings ->
			settings.toBuilder()
				.setFormat(format.toProto)
				.build()
		}
	}

	override suspend fun onFilePrefixChange(prefix: String) {
		dataStore.updateData { settings ->
			settings.toBuilder()
				.setPrefix(prefix)
				.build()
		}
	}

	override suspend fun onAllowExternalFileRead(isAllowed: Boolean) {
		dataStore.updateData { settings ->
			settings.toBuilder()
				.setAllowExternalRead(isAllowed)
				.build()
		}
	}

	override suspend fun onExportItemPrefixChange(prefix: String) {
		dataStore.updateData { settings ->
			settings.toBuilder()
				.setExportedItemPrefix(prefix)
				.build()
		}
	}
}