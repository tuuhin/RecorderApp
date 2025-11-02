package com.eva.bookmarks.data

import android.content.Context
import androidx.core.content.FileProvider
import com.eva.bookmarks.domain.provider.ExportURIProvider
import java.io.File

internal class AndroidExportURIProvider(private val context: Context) : ExportURIProvider {

	override val filesDirectory: File
		get() = File(context.cacheDir, "bookmarks").apply(File::mkdirs)

	override fun getURIFromFile(file: File): String? {
		val contentURI = FileProvider
			.getUriForFile(context, "${context.packageName}.provider", file)
		return contentURI?.toString()
	}
}