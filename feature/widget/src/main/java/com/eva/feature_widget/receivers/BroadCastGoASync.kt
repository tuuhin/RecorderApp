package com.eva.feature_widget.receivers

import android.content.BroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal fun BroadcastReceiver.goAsync(
	coroutineContext: CoroutineContext = Dispatchers.Main,
	code: suspend () -> Unit,
) {
	val scope = CoroutineScope(coroutineContext + SupervisorJob())
	val pendingResult = goAsync()
	scope.launch {
		try {
			code()
		} catch (e: Exception) {
			e.printStackTrace()
		} finally {
			// finish the pending result and cancel the scope
			pendingResult.finish()
			scope.cancel()
		}
	}
}