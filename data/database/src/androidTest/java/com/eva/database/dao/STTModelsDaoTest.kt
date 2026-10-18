package com.eva.database.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNull
import com.eva.database.entity.STTModelEntity
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
class STTModelsDaoTest {

	@Inject
	lateinit var dao: STTModelsDao

	@get:Rule
	val hiltRule = HiltAndroidRule(this)

	// the date column stores milliseconds, so the fixture is truncated to survive the round trip
	private val updatedAt = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
		.toLocalDateTime(TimeZone.currentSystemDefault())

	private fun createModel(
		modelId: String = "model-1",
		status: STTModelEntity.ModelStatus = STTModelEntity.ModelStatus.NOT_READY,
		modelSize: Long = 1024,
	) = STTModelEntity(
		modelId = modelId,
		locale = "en-in",
		externalModelURI = "https://example.com/model.zip",
		externalModelVersion = "v1.0",
		version = 1,
		modelURI = null,
		modelSize = modelSize,
		status = status,
		updatedAt = updatedAt,
	)

	@Before
	fun setUp() = runBlocking {
		hiltRule.inject()
		dao.readAllModelData().forEach { dao.deleteModelEntity(it) }
	}

	@Test
	fun updateOrInsertEntity_inserts_and_reads_back() = runTest {
		val entity = createModel(status = STTModelEntity.ModelStatus.DOWNLOADED)

		val rowId = dao.updateOrInsertEntity(entity)

		assertThat(rowId).isGreaterThan(0L)
		assertThat(dao.readModelById("model-1")).isEqualTo(entity)
	}

	@Test
	fun updateOrInsertEntity_updates_existing_model() = runTest {
		dao.updateOrInsertEntity(createModel(modelSize = 1024))

		dao.updateOrInsertEntity(createModel(modelSize = 2048))

		val stored = dao.readAllModelData()

		assertThat(stored).hasSize(1)
		assertThat(stored.single().modelSize).isEqualTo(2048L)
	}

	@Test
	fun updateOrInsertModelEntities_upserts_all() = runTest {
		dao.updateOrInsertEntity(createModel(modelId = "model-1", modelSize = 1024))

		dao.updateOrInsertModelEntities(
			listOf(createModel(modelId = "model-1", modelSize = 2048), createModel(modelId = "model-2"))
		)

		val sizes = dao.readAllModelData().associate { it.modelId to it.modelSize }

		assertThat(sizes).hasSize(2)
		assertThat(sizes["model-1"]).isEqualTo(2048L)
		assertThat(sizes["model-2"]).isEqualTo(1024L)
	}

	@Test
	fun readModelById_returns_null_for_unknown_id() = runTest {
		assertThat(dao.readModelById("missing")).isNull()
	}

	@Test
	fun readAllModelData_returns_all_models() = runTest {
		dao.updateOrInsertModelEntities(listOf(createModel(modelId = "model-1"), createModel(modelId = "model-2")))

		assertThat(dao.readAllModelData()).hasSize(2)
	}

	@Test
	fun readModelsAsFlow_emits_all_models() = runTest {
		dao.updateOrInsertModelEntities(listOf(createModel(modelId = "model-1"), createModel(modelId = "model-2")))

		assertThat(dao.readModelsAsFlow().first()).hasSize(2)
	}

	@Test
	fun deleteModelEntity_removes_the_model() = runTest {
		val entity = createModel(modelId = "model-1")

		dao.updateOrInsertEntity(entity)

		dao.deleteModelEntity(entity)

		assertThat(dao.readAllModelData()).isEmpty()
	}
}
