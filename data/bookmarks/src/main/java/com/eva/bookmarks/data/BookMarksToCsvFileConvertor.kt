package com.eva.bookmarks.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.eva.bookmarks.domain.AudioBookmarkModel
import com.eva.bookmarks.domain.provider.ExportBookMarkUriProvider
import com.eva.utils.LocalTimeFormats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.format
import java.io.File

private const val TAG = "BOOK_MARKS_EXPORTER"

internal class BookMarksToCsvFileConvertor(private val context: Context) :
	ExportBookMarkUriProvider {

	private val filesDir: File
		// as this files will be a one time export no need to save them to the files dir
		get() = File(context.cacheDir, "bookmarks").apply(File::mkdirs)

	private fun File.toContentUri(): Uri =
		FileProvider.getUriForFile(context, "${context.packageName}.provider", this)

	override suspend operator fun invoke(points: Collection<AudioBookmarkModel>): String? {
		return withContext(Dispatchers.IO) {

			val content = buildString {
				append("BOOKMARK_ID,TEXT,TIMESTAMP\n")
				points.forEach { entry ->
					val readableTimestamp = entry.timeStamp
						.format(LocalTimeFormats.LOCALTIME_HH_MM_SS_FORMAT)
					append("${entry.bookMarkId},${entry.text},$readableTimestamp\n")
				}
			}
			Log.d(TAG, "CONTENT PREPARED")
			Log.d(TAG, content)

			try {
				val file = File(filesDir, "bookmarks.csv").apply(File::createNewFile)

				if (file.exists())
				// just a log message to check if the file is present or not
					Log.d(TAG, "FILE_WAS_PRESENT_THIS_WILL_BE_OVERRIDE")

				// write new contents to the file
				file.writeText(content)
				Log.d(TAG, "FILE_WRITTEN")

				val contentUri = file.toContentUri()

				Log.d(TAG, "CONTENT URI :$contentUri")

				return@withContext contentUri.toString()
			} catch (e: Exception) {
				e.printStackTrace()
				null
			}
		}
	}

}