package com.eva.recordings.domain.provider

import com.eva.recordings.domain.models.DeviceTotalStorageModel

fun interface StorageInfoProvider {

	suspend fun invoke(): Result<DeviceTotalStorageModel>
}