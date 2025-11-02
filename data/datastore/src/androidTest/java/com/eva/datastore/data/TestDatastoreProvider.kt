package com.eva.datastore.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.eva.datastore.data.serializers.FileSettingsSerializer
import com.eva.datastore.data.serializers.RecorderSettingsSerializer
import com.eva.datastore.domain.DataStoreProvider
import com.eva.datastore.proto.FileSettingsProto
import com.eva.datastore.proto.RecorderSettingsProto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class TestDatastoreProvider(private val context: Context) : DataStoreProvider {

	private val _tempDir by lazy {
		File(context.cacheDir, "fake_datastore")
			.apply(File::mkdirs)
	}

	override val preferencesDataStore: DataStore<Preferences>
		get() = PreferenceDataStoreFactory.create { File(_tempDir, "prefs_test.preferences_pb") }

	override val audioSettingsDataStore: DataStore<RecorderSettingsProto>
		get() = DataStoreFactory.create(
			serializer = RecorderSettingsSerializer,
			produceFile = { File(_tempDir, "audio_test.pb") },
		)

	override val fileSettingsDataStore: DataStore<FileSettingsProto>
		get() = DataStoreFactory.create(
			serializer = FileSettingsSerializer,
			produceFile = { File(_tempDir, "file_test.pb") }
		)

	override suspend fun cleanUp() {
		withContext(Dispatchers.IO) {
			try {
				_tempDir.deleteRecursively()
			} catch (_: Exception) {
			}
		}
	}
}