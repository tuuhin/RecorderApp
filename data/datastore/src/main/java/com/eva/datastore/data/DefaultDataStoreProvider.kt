package com.eva.datastore.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.eva.datastore.data.serializers.FileSettingsSerializer
import com.eva.datastore.data.serializers.RecorderSettingsSerializer
import com.eva.datastore.domain.DataStoreProvider
import com.eva.datastore.proto.FileSettingsProto
import com.eva.datastore.proto.RecorderSettingsProto

private val Context.preferences by preferencesDataStore(
	name = DataStoreConstants.PREFERENCES_DATASTORE_FILE
)

private val Context.recorderSettings: DataStore<RecorderSettingsProto> by dataStore(
	fileName = DataStoreConstants.RECORDER_SETTINGS_FILE_NAME,
	serializer = RecorderSettingsSerializer
)

private val Context.recorderFileSettings: DataStore<FileSettingsProto> by dataStore(
	fileName = DataStoreConstants.RECORDER_FILE_SETTINGS_FILE_NAME,
	serializer = FileSettingsSerializer
)

internal class DefaultDataStoreProvider(private val context: Context) : DataStoreProvider {

	override val preferencesDataStore: DataStore<Preferences>
		get() = context.preferences

	override val audioSettingsDataStore: DataStore<RecorderSettingsProto>
		get() = context.recorderSettings

	override val fileSettingsDataStore: DataStore<FileSettingsProto>
		get() = context.recorderFileSettings

	override suspend fun cleanUp() = Unit

}