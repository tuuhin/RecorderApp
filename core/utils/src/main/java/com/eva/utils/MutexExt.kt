package com.eva.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OwnersHoldLockException :
	Exception("Owner is holding the lock cannot perform operation")

suspend inline fun <T> Mutex.tryWithLock(
	owner: Any,
	action: () -> T,
): Result<T> {
	if (holdsLock(owner)) return Result.failure(OwnersHoldLockException())

	return withLock(owner = owner) {
		try {
			Result.success(action())
		} catch (e: Exception) {
			Result.failure(e)
		}
	}
}
