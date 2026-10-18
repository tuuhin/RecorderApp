package com.eva.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNull
import com.eva.database.entity.RecordingsMetaDataEntity
import com.eva.database.entity.RecordingBookMarkEntity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RecordingsBookmarkDaoTest {

	@Inject
	lateinit var dao: RecordingsBookmarkDao

	@Inject
	lateinit var metadataDao: RecordingsMetadataDao

	@get:Rule
	val hiltRule = HiltAndroidRule(this)

	private val timeStamp = LocalTime(hour = 0, minute = 1, second = 30)

	private fun bookmark(
		recordId: Long,
		text: String = "SOME_TEXT",
	) = RecordingBookMarkEntity(
		text = text,
		recordingId = recordId,
		timeStamp = timeStamp,
	)

	@Before
	fun setUp() = runBlocking {
		hiltRule.inject()
		dao.clearAllBookmarkData()
		metadataDao.deleteRecordingsMetaDataBulk(metadataDao.getAllRecordingsMetaDataAsList())
	}

	private suspend fun addRecording(id: Long) {
		metadataDao.updateOrInsertRecordingMetadata(RecordingsMetaDataEntity(recordingId = id))
	}

	@Test
	fun insertOrUpdateBookmark_inserts_and_reads_back() = runTest {
		addRecording(1)

		val rowId = dao.insertOrUpdateBookmark(bookmark(recordId = 1, text = "FIRST"))

		assertThat(rowId).isGreaterThan(0L)
		assertThat(dao.getBookMarkFromBookMarkId(rowId)).isEqualTo(
			bookmark(recordId = 1, text = "FIRST").copy(bookMarkId = rowId),
		)
	}

	@Test
	fun insertOrUpdateBookmark_updates_existing_bookmark() = runTest {
		addRecording(1)

		val rowId = dao.insertOrUpdateBookmark(bookmark(recordId = 1, text = "FIRST"))

		dao.insertOrUpdateBookmark(
			bookmark(recordId = 1, text = "UPDATED").copy(bookMarkId = rowId)
		)

		val updated = dao.getBookMarkFromBookMarkId(rowId)

		assertThat(updated?.text).isEqualTo("UPDATED")
		assertThat(dao.getBookMarksFromRecordingId(1)).hasSize(1)
	}

	@Test
	fun insertOrUpdateBookmarks_upserts_all() = runTest {
		addRecording(1)

		dao.insertOrUpdateBookmarks(
			listOf(bookmark(recordId = 1, text = "FIRST"), bookmark(recordId = 1, text = "SECOND"))
		)

		assertThat(dao.getBookMarksFromRecordingId(1)).hasSize(2)
	}

	@Test
	fun getBookMarksFromRecordingIdAsFlow_emits_only_given_recording() = runTest {
		addRecording(1)
		addRecording(2)

		dao.insertOrUpdateBookmarks(
			listOf(bookmark(recordId = 1, text = "FIRST"), bookmark(recordId = 2, text = "SECOND"))
		)

		val texts = dao.getBookMarksFromRecordingIdAsFlow(1).first().map { it.text }

		assertThat(texts).isEqualTo(listOf("FIRST"))
	}

	@Test
	fun getBookMarksFromRecordingId_returns_only_given_recording() = runTest {
		addRecording(1)
		addRecording(2)

		dao.insertOrUpdateBookmarks(
			listOf(bookmark(recordId = 1, text = "FIRST"), bookmark(recordId = 2, text = "SECOND"))
		)

		assertThat(dao.getBookMarksFromRecordingId(2).map { it.text }).isEqualTo(listOf("SECOND"))
	}

	@Test
	fun getBookMarkFromBookMarkId_returns_null_for_unknown_id() = runTest {
		assertThat(dao.getBookMarkFromBookMarkId(bookmarkId = 999)).isNull()
	}

	@Test
	fun deleteBookMarksByRecordingId_removes_only_that_recording() = runTest {
		addRecording(1)
		addRecording(2)

		dao.insertOrUpdateBookmarks(
			listOf(bookmark(recordId = 1, text = "FIRST"), bookmark(recordId = 2, text = "SECOND"))
		)

		dao.deleteBookMarksByRecordingId(1)

		assertThat(dao.getBookMarksFromRecordingId(1)).isEmpty()
		assertThat(dao.getBookMarksFromRecordingId(2).map { it.text }).isEqualTo(listOf("SECOND"))
	}

	@Test
	fun deleteBookMark_removes_single_bookmark() = runTest {
		addRecording(1)

		val rowId = dao.insertOrUpdateBookmark(bookmark(recordId = 1, text = "FIRST"))

		dao.deleteBookMark(bookmark(recordId = 1, text = "FIRST").copy(bookMarkId = rowId))

		assertThat(dao.getBookMarksFromRecordingId(1)).isEmpty()
	}

	@Test
	fun deleteBookMarks_removes_given_bookmarks() = runTest {
		addRecording(1)

		val firstId = dao.insertOrUpdateBookmark(bookmark(recordId = 1, text = "FIRST"))
		dao.insertOrUpdateBookmark(bookmark(recordId = 1, text = "SECOND"))
		val thirdId = dao.insertOrUpdateBookmark(bookmark(recordId = 1, text = "THIRD"))

		dao.deleteBookMarks(
			listOf(
				bookmark(recordId = 1, text = "FIRST").copy(bookMarkId = firstId),
				bookmark(recordId = 1, text = "THIRD").copy(bookMarkId = thirdId),
			)
		)

		assertThat(dao.getBookMarksFromRecordingId(1).map { it.text }).isEqualTo(listOf("SECOND"))
	}

	@Test
	fun clearAllBookmarkData_empties_table() = runTest {
		addRecording(1)

		dao.insertOrUpdateBookmark(bookmark(recordId = 1, text = "FIRST"))

		dao.clearAllBookmarkData()

		assertThat(dao.getBookMarksFromRecordingId(1)).isEmpty()
	}
}
