package com.eva.interactions.data.bluetooth

import android.media.AudioDeviceInfo
import com.eva.interactions.domain.models.AudioDevice

internal fun AudioDeviceInfo.toModel() = AudioDevice(
	id = id,
	productName = productName?.toString()
)
