package com.eva.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNull
import com.eva.database.entity.RecordingsMetaDataEntity
import com.eva.database.entity.RecordingCategoryEntity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RecordingCategoryDaoTest {

	@Inject
	lateinit var dao: RecordingCategoryDao

	@Inject
	lateinit var metadataDao: RecordingsMetadataDao

	@get:Rule
	val hiltRule = HiltAndroidRule(this)

	// the date column stores milliseconds, so the fixture is truncated to survive the round trip
	private val randomDate = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
		.toLocalDateTime(TimeZone.currentSystemDefault())

	private fun createCategory(
		name: String,
		categoryId: Long? = null,
		color: String? = null,
	) = RecordingCategoryEntity(
		categoryId = categoryId,
		categoryName = name,
		createdAt = randomDate,
		color = color,
	)

	@Before
	fun setUp() = runBlocking {
		hiltRule.inject()
		metadataDao.deleteRecordingsMetaDataBulk(metadataDao.getAllRecordingsMetaDataAsList())
		dao.deleteAllCategories()
	}

	@Test
	fun insertOrUpdateCategory_inserts_new_category() = runTest {
		val entity = createCategory(name = "WORK")

		val rowId = dao.insertOrUpdateCategory(entity)

		assertThat(rowId).isGreaterThan(0L)
		assertThat(dao.getCategoryFromId(rowId)).isEqualTo(entity.copy(categoryId = rowId))
	}

	@Test
	fun insertOrUpdateCategory_updates_existing_category() = runTest {
		val rowId = dao.insertOrUpdateCategory(createCategory(name = "WORK"))

		dao.insertOrUpdateCategory(createCategory(name = "PERSONAL", categoryId = rowId, color = "#FF0000"))

		val updated = dao.getCategoryFromId(rowId)

		assertThat(updated?.categoryName).isEqualTo("PERSONAL")
		assertThat(updated?.color).isEqualTo("#FF0000")
		assertThat(dao.getAllCategoryAsList()).hasSize(1)
	}

	@Test
	fun addCategoriesBulk_skips_duplicate_names() = runTest {
		dao.addCategoriesBulk(
			listOf(
				createCategory(name = "WORK"),
				createCategory(name = "PERSONAL"),
				createCategory(name = "WORK"),
			)
		)

		assertThat(dao.getAllCategoryAsList()).hasSize(2)
	}

	@Test
	fun getCategoryFromId_returns_null_for_unknown_id() = runTest {
		assertThat(dao.getCategoryFromId(id = 999)).isNull()
	}

	@Test
	fun getAllCategoryAsFlow_emits_all_categories() = runTest {
		dao.addCategoriesBulk(listOf(createCategory(name = "WORK"), createCategory(name = "PERSONAL")))

		val names = dao.getAllCategoryAsFlow().first()
			.map { it.categoryName }
			.toSet()

		assertThat(names).isEqualTo(setOf("WORK", "PERSONAL"))
	}

	@Test
	fun getAllCategoryAsList_returns_all_categories() = runTest {
		dao.addCategoriesBulk(listOf(createCategory(name = "WORK"), createCategory(name = "PERSONAL")))

		assertThat(dao.getAllCategoryAsList()).hasSize(2)
	}

	@Test
	fun getCategoriesAsFlowWithCount_counts_recordings_per_category() = runTest {
		val workId = dao.insertOrUpdateCategory(createCategory(name = "WORK"))
		val personalId = dao.insertOrUpdateCategory(createCategory(name = "PERSONAL"))
		dao.insertOrUpdateCategory(createCategory(name = "UNUSED"))

		metadataDao.updateOrInsertRecordingMetadataBulk(
			listOf(
				RecordingsMetaDataEntity(recordingId = 1, categoryId = workId),
				RecordingsMetaDataEntity(recordingId = 2, categoryId = workId),
				RecordingsMetaDataEntity(recordingId = 3, categoryId = personalId),
				RecordingsMetaDataEntity(recordingId = 4),
			)
		)

		val counts = dao.getCategoriesAsFlowWithCount().first()
			.entries
			.associate { (entity, count) -> entity.categoryName to count }

		assertThat(counts).hasSize(3)
		assertThat(counts["WORK"]).isEqualTo(2L)
		assertThat(counts["PERSONAL"]).isEqualTo(1L)
		assertThat(counts["UNUSED"]).isEqualTo(0L)
	}

	@Test
	fun deleteCategory_keeps_recordings_with_null_category() = runTest {
		val workId = dao.insertOrUpdateCategory(createCategory(name = "WORK"))

		metadataDao.updateOrInsertRecordingMetadata(
			RecordingsMetaDataEntity(recordingId = 1, categoryId = workId)
		)

		dao.deleteCategory(createCategory(name = "WORK", categoryId = workId))

		assertThat(metadataDao.getRecordingMetaDataFromId(1)?.categoryId).isNull()
	}

	@Test
	fun deleteCategoriesBulk_removes_only_given_categories() = runTest {
		val workId = dao.insertOrUpdateCategory(createCategory(name = "WORK"))
		val personalId = dao.insertOrUpdateCategory(createCategory(name = "PERSONAL"))
		dao.insertOrUpdateCategory(createCategory(name = "OTHER"))

		dao.deleteCategoriesBulk(
			listOf(
				createCategory(name = "WORK", categoryId = workId),
				createCategory(name = "PERSONAL", categoryId = personalId),
			)
		)

		assertThat(dao.getAllCategoryAsList().map { it.categoryName }).isEqualTo(listOf("OTHER"))
	}

	@Test
	fun deleteAllCategories_empties_table() = runTest {
		dao.addCategoriesBulk(listOf(createCategory(name = "WORK"), createCategory(name = "PERSONAL")))

		dao.deleteAllCategories()

		assertThat(dao.getAllCategoryAsList()).isEmpty()
	}
}
