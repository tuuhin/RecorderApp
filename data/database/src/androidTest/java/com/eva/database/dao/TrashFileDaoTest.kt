package com.eva.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import com.eva.database.entity.TrashFileEntity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
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
class TrashFileDaoTest {

	@Inject
	lateinit var dao: TrashFileDao

	@get:Rule
	val hiltRule = HiltAndroidRule(this)

	// the date column stores milliseconds, so the fixture is truncated to survive the round trip
	private val randomDate = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
		.toLocalDateTime(TimeZone.currentSystemDefault())

	private fun trashFile(
		id: Long,
		title: String = "TRASH_ENTRY",
		expiresAt: LocalDateTime? = null,
	) = TrashFileEntity(
		id = id,
		title = title,
		displayName = "DISPLAY_NAME_$id",
		mimeType = "audio/*",
		dateAdded = randomDate,
		expiresAt = expiresAt,
		file = "some_location_$id",
	)

	@Before
	fun setUp() = runBlocking {
		hiltRule.inject()
		dao.deleteAllTrashEntries()
	}

	@Test
	fun addNewTrashFile_inserts_and_reads_back() = runTest {
		val entity = trashFile(id = 1)

		val rowId = dao.addNewTrashFile(entity)

		assertThat(rowId).isGreaterThan(0L)
		assertThat(dao.getAllTrashFiles()).isEqualTo(listOf(entity))
	}

	@Test
	fun addNewTrashFile_replaces_row_with_same_id() = runTest {
		dao.addNewTrashFile(trashFile(id = 1, title = "ORIGINAL"))
		dao.addNewTrashFile(trashFile(id = 1, title = "REPLACED"))

		val files = dao.getAllTrashFiles()

		assertThat(files).hasSize(1)
		assertThat(files.single().title).isEqualTo("REPLACED")
	}

	@Test
	fun getAllTrashFilesFlow_emits_all_rows() = runTest {
		val first = trashFile(id = 1)
		val second = trashFile(id = 2, expiresAt = randomDate)

		dao.addNewTrashFiles(listOf(first, second))

		assertThat(dao.getAllTrashFilesFlow().first()).isEqualTo(listOf(first, second))
	}

	@Test
	fun addNewTrashFiles_ignores_duplicates() = runTest {
		dao.addNewTrashFiles(
			listOf(
				trashFile(id = 1, title = "FIRST"),
				trashFile(id = 2),
				trashFile(id = 1, title = "SECOND"),
			)
		)

		val files = dao.getAllTrashFiles().sortedBy { it.id }

		assertThat(files).hasSize(2)
		assertThat(files.first { it.id == 1L }.title).isEqualTo("FIRST")
	}

	@Test
	fun deleteTrashEntityFromId_removes_only_that_row() = runTest {
		dao.addNewTrashFiles(listOf(trashFile(id = 1), trashFile(id = 2)))

		dao.deleteTrashEntityFromId(1)

		assertThat(dao.getAllTrashFiles()).isEqualTo(listOf(trashFile(id = 2)))
	}

	@Test
	fun deleteTrashEntity_removes_the_row() = runTest {
		dao.addNewTrashFile(trashFile(id = 1))

		dao.deleteTrashEntity(trashFile(id = 1))

		assertThat(dao.getAllTrashFiles()).isEmpty()
	}
}
