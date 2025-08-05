package com.eva.recordings.data.provider

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.storage.StorageManager
import androidx.core.content.getSystemService
import com.eva.recordings.domain.models.DeviceTotalStorageModel
import com.eva.recordings.domain.provider.StorageInfoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.IOException

internal class StorageInfoProviderImpl(
	private val context: Context
) : StorageInfoProvider {

	private val _manager by lazy { context.getSystemService<StorageStatsManager>() }

	override suspend fun invoke(): Result<DeviceTotalStorageModel> {
		return withContext(Dispatchers.IO) {
			try {
				val freeSpace = async { _manager?.getFreeBytes(StorageManager.UUID_DEFAULT) ?: 0L }
				val totalSpace = async { _manager?.getTotalBytes(StorageManager.UUID_DEFAULT) ?: 0L }

				val model = DeviceTotalStorageModel(
					totalAmountInBytes = totalSpace.await(),
					freeAmountInBytes = freeSpace.await()
				)
				Result.success(model)
			} catch (e: IOException) {
				Result.failure(e)
			}
		}
	}

}