package com.eva.bookmarks.data

import android.util.Log
import com.eva.bookmarks.domain.AudioBookmarkModel
import com.eva.bookmarks.domain.provider.BookMarksExportRepository
import com.eva.bookmarks.domain.provider.ExportURIProvider
import com.eva.utils.LocalTimeFormats
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.datetime.format
import java.io.File
import java.util.UUID

private const val TAG = "BOOK_MARKS_EXPORTER"

internal class BookMarksToCsvFileConvertor(
	private val provider: ExportURIProvider
) : BookMarksExportRepository {

	override suspend fun invoke(points: List<AudioBookmarkModel>): String? {

		val fileName = "bookmarks_${UUID.randomUUID()}.csv"
		val file = File(provider.filesDirectory, fileName)

		if (points.isEmpty()) {
			Log.e(TAG, "EMPTY FILE CONTENT")
			return null
		}

		return try {
			withContext(Dispatchers.IO) {
				val isNewFile = file.createNewFile()
				Log.d(TAG, "IS NEW FILE :$isNewFile")

				file.outputStream().bufferedWriter().use { writer ->
					writer.write("\"BOOKMARK_ID\",\"TEXT\",\"TIMESTAMP\"")
					writer.newLine()
					points.forEach { entry ->
						val timestamp =
							entry.timeStamp.format(LocalTimeFormats.LOCALTIME_HH_MM_SS_FORMAT)
						writer.write(escapeCsv("${entry.bookMarkId},${entry.text},$timestamp"))
						writer.newLine()
					}
				}
				Log.d(TAG, "CONTENT COPIED TO FILE")
				val contentUri = provider.getURIFromFile(file)
				contentUri.toString()
			}
		} catch (e: CancellationException) {
			withContext(NonCancellable) {
				if (file.exists()) file.delete()
			}
			throw e
		} catch (e: Exception) {
			e.printStackTrace()
			null
		}
	}

	private fun escapeCsv(value: String) =
		"\"${value.replace("\"", "\"\"")}\""
}