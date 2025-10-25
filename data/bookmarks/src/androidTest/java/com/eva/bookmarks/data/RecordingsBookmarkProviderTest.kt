package com.eva.bookmarks.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.eva.bookmarks.domain.AudioBookmarkModel
import com.eva.bookmarks.domain.provider.RecordingBookmarksProvider
import com.eva.database.dao.RecordingsBookmarkDao
import com.eva.recordings.domain.exceptions.InvalidRecordingIdException
import com.eva.utils.Resource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class RecordingsBookmarkProviderTest {

	@get:Rule
	val hiltRule = HiltAndroidRule(this)

	@Inject
	lateinit var provider: RecordingBookmarksProvider

	@Inject
	lateinit var bookmarkDao: RecordingsBookmarkDao

	@BeforeTest
	fun setUp() = hiltRule.inject()

	@AfterTest
	fun tearDown() = runBlocking {
		bookmarkDao.clearAllBookmarkData()
	}

	@Test
	fun create_bookMark_single_entity_successfully() = runTest {
		val recordingId = 0L

		val time = LocalTime(0, 0)
		val text = "Start Point"

		val result = provider.createBookMark(recordingId, time, text)

		assertTrue(result is Resource.Success)
		val bookMarks = provider.getRecordingBookmarksFromIdAsList(recordingId)
		assertEquals(1, bookMarks.size)
	}

	@Test
	fun create_bookmark_multiple_entries_successfully() = runTest {
		val recordingId = 0L
		val points = List(10) { LocalTime(it, 0) }
		provider.createBookMarks(recordingId, points)

		val result = provider.getRecordingBookmarksFromIdAsList(recordingId)
		assertEquals(10, result.size)
	}

	@Test
	fun update_bookMark_successfully() = runTest {
		val recordingId = 0L
		val oldTextValue = "Some Value"
		val timeStamp = LocalTime(0, 0)
		val result = provider.createBookMark(recordingId, timeStamp, text = oldTextValue)

		assertTrue(result is Resource.Success, "NEW ENTRY ADDED")

		val newText = "New label"
		provider.updateBookMark(result.data, newText)
		val updated = provider.getRecordingBookmarksFromIdAsList(recordingId).firstOrNull()

		assertEquals(newText, updated?.text, "The updated enty has the updated text")
	}

	@Test
	fun updating_bookMark_with_invalid_id_creates_new_bookmark() = runTest {
		val invalidBookmark = AudioBookmarkModel(
			bookMarkId = 9999L,
			recordingId = 100L,
			timeStamp = LocalTime(0, 2),
			text = "Invalid test"
		)
		val result = provider.updateBookMark(invalidBookmark, "Updated-test")

		assertTrue(result is Resource.Error, "Cannot update the bookmark")
		assertIs<InvalidRecordingIdException>(
			result.error,
			"Recording id was not present thus invalid recording id"
		)
	}

	@Test
	fun checking_adding_new_bookmarks_flow() = runTest {
		val recordingId = 0L
		provider.getRecordingBookmarksFromId(recordingId).test {
			val firstEmit = awaitItem()

			assertTrue(firstEmit.isEmpty())

			provider.createBookMark(recordingId, LocalTime(0, 0), "Something")
			val secondEmit = awaitItem()

			assertTrue(secondEmit.isNotEmpty())

			cancelAndIgnoreRemainingEvents()
		}
	}

	@Test
	fun checking_bookmarks_add_update_delete_operations() = runTest {
		val recordingId = 0L

		// create new
		val newBookMark = provider.createBookMark(recordingId, LocalTime(0, 0), "Something")
		assertTrue(newBookMark is Resource.Success, "Create New bookmark is success")

		// read the number of items
		val items1 = provider.getRecordingBookmarksFromIdAsList(recordingId)
		assertEquals(1, items1.size, "New entry so size is 1")

		val updatedName = "New Name"

		// update
		val updatedResult = provider.updateBookMark(newBookMark.data, updatedName)
		assertTrue(updatedResult is Resource.Success, "Bookmark updated")
		assertEquals(updatedName, updatedResult.data.text, "The updated the item ")

		//delete
		val deleteResult = provider.deleteBookMarks(listOf(updatedResult.data))
		assertTrue(deleteResult is Resource.Success, "Bookmark deleted")

		val items2 = provider.getRecordingBookmarksFromIdAsList(recordingId)
		assertEquals(0, items2.size, "Old item deleted so 0")

	}

	@Test
	fun deleting_bookMarks() = runTest {
		val recordingId = 0L
		val points = List(10) { LocalTime(it, 0) }
		provider.createBookMarks(recordingId, points)

		val newlyAdded = provider.getRecordingBookmarksFromIdAsList(recordingId)

		assertEquals(10, newlyAdded.size)

		val deleteResult = provider.deleteBookMarks(newlyAdded)

		assertTrue(deleteResult is Resource.Success)

		val bookMarkList = provider.getRecordingBookmarksFromIdAsList(recordingId)

		assertTrue(bookMarkList.isEmpty())
	}

}