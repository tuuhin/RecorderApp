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
import com.eva.database.entity.RecordingCategoryEntity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RecordingsMetadataDaoTest {

	@Inject
	lateinit var dao: RecordingsMetadataDao

	@Inject
	lateinit var categoryDao: RecordingCategoryDao

	@Inject
	lateinit var bookmarkDao: RecordingsBookmarkDao

	@get:Rule
	val hiltRule = HiltAndroidRule(this)

	private fun createRecording(
		id: Long,
		isFavourite: Boolean = false,
		categoryId: Long? = null,
	) = RecordingsMetaDataEntity(
		recordingId = id,
		isFavourite = isFavourite,
		categoryId = categoryId,
	)

	@Before
	fun setUp() = runBlocking {
		hiltRule.inject()
		bookmarkDao.clearAllBookmarkData()
		dao.deleteRecordingsMetaDataBulk(dao.getAllRecordingsMetaDataAsList())
		categoryDao.deleteAllCategories()
	}

	@Test
	fun updateOrInsertRecordingMetadata_inserts_and_reads_back() = runTest {
		val entity = createRecording(id = 1, isFavourite = true)

		val rowId = dao.updateOrInsertRecordingMetadata(entity)

		assertThat(rowId).isGreaterThan(0L)
		assertThat(dao.getRecordingMetaDataFromId(1)).isEqualTo(entity)
	}

	@Test
	fun updateOrInsertRecordingMetadata_updates_existing_row() = runTest {
		dao.updateOrInsertRecordingMetadata(createRecording(id = 1, isFavourite = false))

		dao.updateOrInsertRecordingMetadata(createRecording(id = 1, isFavourite = true))

		assertThat(dao.getRecordingMetaDataFromId(1)?.isFavourite).isEqualTo(true)
		assertThat(dao.getAllRecordingsMetaDataAsList()).hasSize(1)
	}

	@Test
	fun updateOrInsertRecordingMetadataBulk_upserts_all() = runTest {
		dao.updateOrInsertRecordingMetadata(createRecording(id = 1, isFavourite = false))

		dao.updateOrInsertRecordingMetadataBulk(
			listOf(createRecording(id = 1, isFavourite = true), createRecording(id = 2))
		)

		val stored = dao.getAllRecordingsMetaDataAsList().sortedBy { it.recordingId }

		assertThat(stored).hasSize(2)
		assertThat(stored.first().isFavourite).isEqualTo(true)
	}

	@Test
	fun addRecordingMetaDataBulk_ignores_existing_rows() = runTest {
		dao.updateOrInsertRecordingMetadata(createRecording(id = 1, isFavourite = false))

		dao.addRecordingMetaDataBulk(
			listOf(createRecording(id = 1, isFavourite = true), createRecording(id = 2))
		)

		val stored = dao.getAllRecordingsMetaDataAsList().sortedBy { it.recordingId }

		assertThat(stored).hasSize(2)
		// the existing favourite flag is kept, the IGNORE strategy did not update it
		assertThat(stored.first().isFavourite).isEqualTo(false)
	}

	@Test
	fun getAllRecordingsMetaDataAsFlow_emits_all_rows() = runTest {
		dao.updateOrInsertRecordingMetadataBulk(listOf(createRecording(id = 1), createRecording(id = 2)))

		assertThat(dao.getAllRecordingsMetaDataAsFlow().first()).hasSize(2)
	}

	@Test
	fun getAllRecordingsMetaDataAsList_returns_all_rows() = runTest {
		dao.updateOrInsertRecordingMetadataBulk(listOf(createRecording(id = 1), createRecording(id = 2)))

		assertThat(dao.getAllRecordingsMetaDataAsList()).hasSize(2)
	}

	@Test
	fun getRecordingMetaDataFromId_returns_null_for_unknown_id() = runTest {
		assertThat(dao.getRecordingMetaDataFromId(999)).isNull()
	}

	@Test
	fun getRecordingMetaDataFromIdAsFlow_emits_the_entity() = runTest {
		val entity = createRecording(id = 1, isFavourite = true)

		dao.updateOrInsertRecordingMetadata(entity)

		assertThat(dao.getRecordingMetaDataFromIdAsFlow(1).first()).isEqualTo(entity)
	}

	@Test
	fun getRecordingMetaDataFromIds_filters_by_ids() = runTest {
		dao.updateOrInsertRecordingMetadataBulk(
			listOf(createRecording(id = 1), createRecording(id = 2), createRecording(id = 3))
		)

		val ids = dao.getRecordingMetaDataFromIds(listOf(1, 3))
			.map { it.recordingId }
			.sorted()

		assertThat(ids).isEqualTo(listOf(1L, 3L))
	}

	@Test
	fun getRecordingsFromCategoryIdAsFlow_filters_by_category() = runTest {
		val workId = categoryDao.insertOrUpdateCategory(
			RecordingCategoryEntity(
				categoryName = "WORK",
				createdAt = LocalDateTime(
					year = 2024,
					month = Month.JANUARY,
					day = 15,
					hour = 10,
					minute = 30,
				),
			)
		)

		dao.updateOrInsertRecordingMetadataBulk(
			listOf(
				createRecording(id = 1, categoryId = workId),
				createRecording(id = 2, categoryId = workId),
				createRecording(id = 3),
			)
		)

		val ids = dao.getRecordingsFromCategoryIdAsFlow(workId).first()
			.map { it.recordingId }
			.sorted()

		assertThat(ids).isEqualTo(listOf(1L, 2L))
	}

	@Test
	fun checkRecordingWithIdExists_returns_one_for_existing_and_zero_otherwise() = runTest {
		dao.updateOrInsertRecordingMetadata(createRecording(id = 1))

		val exists = withContext(Dispatchers.IO) { dao.checkRecordingWithIdExists(1) }
		val missing = withContext(Dispatchers.IO) { dao.checkRecordingWithIdExists(99) }

		assertThat(exists).isEqualTo(1)
		assertThat(missing).isEqualTo(0)
	}

	@Test
	fun deleteRecordingsMetaDataBulk_removes_given_rows() = runTest {
		dao.updateOrInsertRecordingMetadataBulk(
			listOf(createRecording(id = 1), createRecording(id = 2), createRecording(id = 3))
		)

		dao.deleteRecordingsMetaDataBulk(listOf(createRecording(id = 1), createRecording(id = 3)))

		assertThat(dao.getAllRecordingsMetaDataAsList().map { it.recordingId }).isEqualTo(listOf(2L))
	}

	@Test
	fun deleteRecordingMetadata_cascades_to_bookmarks() = runTest {
		dao.updateOrInsertRecordingMetadata(createRecording(id = 1))

		bookmarkDao.insertOrUpdateBookmark(
			RecordingBookMarkEntity(
				text = "FIRST",
				recordingId = 1,
				timeStamp = LocalTime(hour = 0, minute = 1, second = 30),
			)
		)

		dao.deleteRecordingMetadata(createRecording(id = 1))

		assertThat(dao.getRecordingMetaDataFromId(1)).isNull()
		assertThat(bookmarkDao.getBookMarksFromRecordingId(1)).isEmpty()
	}

	@Test
	fun deleteRecordingMetaDataFromIds_removes_only_given_ids() = runTest {
		dao.updateOrInsertRecordingMetadataBulk(
			listOf(createRecording(id = 1), createRecording(id = 2), createRecording(id = 3))
		)

		dao.deleteRecordingMetaDataFromIds(listOf(1, 3))

		assertThat(dao.getAllRecordingsMetaDataAsList().map { it.recordingId }).isEqualTo(listOf(2L))
	}
}
