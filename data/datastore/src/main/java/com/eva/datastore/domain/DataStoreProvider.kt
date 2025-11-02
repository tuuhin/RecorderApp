package com.eva.datastore.domain

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.eva.datastore.proto.FileSettingsProto
import com.eva.datastore.proto.RecorderSettingsProto
import org.jetbrains.annotations.VisibleForTesting

interface DataStoreProvider {

	val preferencesDataStore: DataStore<Preferences>

	val audioSettingsDataStore: DataStore<RecorderSettingsProto>

	val fileSettingsDataStore: DataStore<FileSettingsProto>

	/**
	 * Cleans up the generated file in testing phase no need to consider this function for
	 * non test scope
	 */
	@VisibleForTesting
	suspend fun cleanUp()
}