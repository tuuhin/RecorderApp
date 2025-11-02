package com.eva.bookmarks.domain.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import java.io.File

interface ExportURIProvider {

	val filesDirectory: File

	fun getURIFromFile(file: File): String?

	@VisibleForTesting
	suspend fun clearAll(): Result<Boolean> {
		return runCatching {
			withContext(Dispatchers.IO) {
				filesDirectory.deleteRecursively()
			}
		}
	}
}