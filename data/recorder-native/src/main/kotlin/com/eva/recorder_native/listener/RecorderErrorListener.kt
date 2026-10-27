package com.eva.recorder_native.listener

internal interface RecorderErrorListener {

	fun onError(error: Exception, code: Int)
}