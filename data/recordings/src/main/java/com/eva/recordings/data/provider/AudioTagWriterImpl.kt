package com.eva.recordings.data.provider

import android.util.Log
import com.eva.recordings.domain.provider.AudioTagWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.kt.AudioTagger
import org.jaudiotagger.kt.CannotWriteException
import org.jaudiotagger.kt.tag.FieldKey
import java.io.File
import kotlinx.io.files.Path as KotlinxIoPath

internal class AudioTagWriterImpl : AudioTagWriter {

	override suspend fun writeAuthorTag(file: File, artistName: String?): Boolean {
		val filePath = KotlinxIoPath(file.path)
		try {
			withContext(Dispatchers.IO) {
				// update the author name if applicable
				val ref = AudioTagger.read(filePath)
				if (artistName != null) {
					Log.d(TAG, "ARTIST NAME INCLUDED")
					ref.tag.set(FieldKey.ARTIST, artistName)
				} else {
					Log.d(TAG, "ARTIST NAME REMOVED")
					ref.tag.remove(FieldKey.ARTIST)
				}
				Log.i(TAG, "AUDIO META FIELDS UPDATED")
				AudioTagger.write(filePath, ref.tag)
			}
			return true
		} catch (_: CannotWriteException) {
			Log.w(TAG, "FAILED TO WRITE THE ARTIST")
		} catch (e: Exception) {
			Log.e(TAG, "FAILED TO SAVE THE AUTHOR NAME", e)
		}
		return false
	}

	companion object {
		private const val TAG = "AUDIO_TAG_WRITER"
	}
}