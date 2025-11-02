package com.eva.bookmarks.data.data

import android.content.Context
import com.eva.bookmarks.domain.provider.ExportURIProvider
import java.io.File

class TestExportURIProvider(private val context: Context) : ExportURIProvider {

	override val filesDirectory: File
		get() = File(context.cacheDir, "test_bookmarks")
			.apply(File::mkdirs)

	override fun getURIFromFile(file: File): String? {
		return file.toString()
	}
}