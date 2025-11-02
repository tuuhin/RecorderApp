package com.eva.location.domain.utils

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
	addOnSuccessListener { result ->
		if (cont.isActive) cont.resume(result)
	}
	addOnFailureListener { exception ->
		if (cont.isActive) cont.resumeWithException(exception)
	}
	addOnCanceledListener {
		if (cont.isActive) cont.cancel()
	}
}