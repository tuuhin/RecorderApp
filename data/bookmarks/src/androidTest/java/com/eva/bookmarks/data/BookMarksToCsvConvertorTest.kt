package com.eva.bookmarks.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eva.bookmarks.domain.AudioBookmarkModel
import com.eva.bookmarks.domain.provider.BookMarksExportRepository
import com.eva.bookmarks.domain.provider.ExportURIProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BookMarksToCsvConvertorTest {

	@get:Rule
	val hiltRule = HiltAndroidRule(this)

	@Inject
	lateinit var exporter: BookMarksExportRepository

	@Inject
	lateinit var uriProvider: ExportURIProvider

	@BeforeTest
	fun setUp() = hiltRule.inject()

	@AfterTest
	fun tearDown() = runBlocking {
		uriProvider.clearAll()
		Unit
	}

	@Test
	fun check_if_export_bookmarks_creates_a_file() = runTest {

		val entries = List(20) {
			AudioBookmarkModel(
				bookMarkId = it.toLong(),
				text = "A bookmark for $it",
				recordingId = 0,
				timeStamp = LocalTime.fromSecondOfDay(it * 10 + 40)
			)
		}

		val uriString = exporter.invoke(entries)

		assertTrue(uriString != null, "A CSV File created")
	}

	@Test
	fun check_no_bookmark_file_created_if_bookmarks_empty() = runTest {

		val entries = emptyList<AudioBookmarkModel>()
		val uriString = exporter.invoke(entries)

		assertEquals(null, uriString, "Doesn't create a file as there is no entries")
	}

}