package com.eva.interactions

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eva.bookmarks.domain.AudioBookmarkModel
import com.eva.interactions.domain.ShareRecordingsUtil
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.utils.Resource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.`is`
import org.junit.Rule
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ShareRecordingsIntentTest {

	lateinit var context: Context

	@get:Rule
	val intentRule = IntentsRule()

	@get:Rule
	val hiltRule = HiltAndroidRule(this)

	@Inject
	lateinit var shareUtil: ShareRecordingsUtil

	@BeforeTest
	fun setup() {
		context = ApplicationProvider.getApplicationContext()
		hiltRule.inject()
	}

	private fun File.toContentURI() =
		FileProvider.getUriForFile(context, "${context.packageName}.provider", this)

	@Test
	fun check_if_sharing_intent_is_shown_for_single_audio_file() = runTest {

		val someFile = withContext(Dispatchers.IO) {
			File(context.cacheDir, "some.mp3").apply { createNewFile() }
		}

		val contentURI = someFile.toContentURI()

		val audioFile = AudioFileModel(
			id = 0L,
			title = "Voice_001",
			displayName = "Voice_001.abc",
			duration = 5.minutes,
			size = 1024 * 20,
			lastModified = LocalDateTime.now().toKotlinLocalDateTime(),
			fileUri = contentURI.toString(),
			mimeType = "audio/mp3",
			path = "",
		)

		try {
			val result = shareUtil.shareAudioFile(audioFile)
			assertTrue(result is Resource.Success, message = "Intent was successfully launched")

			Intents.intended(
				allOf(
					hasAction(Intent.ACTION_CHOOSER),
					hasExtra(
						`is`(Intent.EXTRA_INTENT),
						allOf(
							hasAction(Intent.ACTION_SEND),
							hasType("audio/*")
						)
					)
				)
			)

		} finally {
			// clear the uri when done
			withContext(Dispatchers.IO) {
				someFile.delete()
			}
		}
	}

	@Test
	fun check_if_sharing_intent_is_shown_for_list_of_recordings() = runTest {

		val someFile = withContext(Dispatchers.IO) {
			File(context.cacheDir, "some.mp3").apply { createNewFile() }
		}

		val contentURI = FileProvider.getUriForFile(
			context,
			"${context.packageName}.provider",
			someFile
		)

		val fakeModel = RecordedVoiceModel(
			id = 0L,
			title = "Voice_001",
			displayName = "Voice_001.abc",
			duration = 5.minutes,
			sizeInBytes = 1024 * 20,
			modifiedAt = LocalDateTime.now().toKotlinLocalDateTime(),
			recordedAt = LocalDateTime.now().toKotlinLocalDateTime(),
			fileUri = contentURI.toString(),
			mimeType = "audio/mp3"
		)

		val models = List(10) { fakeModel.copy(id = it.toLong()) }

		try {
			val result = shareUtil.shareAudioFiles(models)
			assertTrue(result is Resource.Success, message = "Intent was successfully launched")

			Intents.intended(
				allOf(
					hasAction(Intent.ACTION_CHOOSER),
					hasExtra(
						`is`(Intent.EXTRA_INTENT),
						allOf(
							hasAction(Intent.ACTION_SEND_MULTIPLE),
							hasType("audio/*")
						)
					)
				)
			)

		} finally {
			// clear the uri when done
			withContext(Dispatchers.IO) {
				someFile.delete()
			}
		}
	}

	@Test
	fun check_sharing_intent_for_bookmarks_shown() = runTest {

		val bookMarks = List(10) {
			AudioBookmarkModel(
				bookMarkId = it.toLong(),
				text = "Some test",
				recordingId = 0,
				timeStamp = LocalTime(0, it)
			)
		}
		val result = shareUtil.shareBookmarksCsv(bookMarks)

		assertTrue(result is Resource.Success, message = "Bookmark intent is launched")

		Intents.intended(
			allOf(
				hasAction(Intent.ACTION_CHOOSER),
				hasExtra(
					`is`(Intent.EXTRA_INTENT),
					allOf(
						hasAction(Intent.ACTION_SEND),
						hasType("text/csv")
					)
				)
			)
		)
	}
}