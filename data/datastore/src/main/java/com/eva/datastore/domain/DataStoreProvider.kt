package com.eva.datastore.domain

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.eva.datastore.proto.FileSettingsProto
import com.eva.datastore.proto.RecorderSettingsProto
import com.eva.datastore.proto.RecordingListSettingsProto
import com.eva.datastore.proto.TranscriptionSettingsProto
import org.jetbrains.annotations.VisibleForTesting

internal interface DataStoreProvider {

	val preferencesDataStore: DataStore<Preferences>

	val audioSettingsDataStore: DataStore<RecorderSettingsProto>

	val fileSettingsDataStore: DataStore<FileSettingsProto>

	val transcriptionsDataStore: DataStore<TranscriptionSettingsProto>

	val recordingsListSettingsDataStore: DataStore<RecordingListSettingsProto>

	/**
	 * Cleans up the generated file in testing phase no need to consider this function for
	 * non-test scope
	 */
	@VisibleForTesting
	suspend fun cleanUp()
}