package com.eva.database

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.time.Clock

private const val TEST_DB_NAME = "migration-test-db"

@RunWith(AndroidJUnit4::class)
class DBMigrationsTests {

	private val instrumentation by lazy { InstrumentationRegistry.getInstrumentation() }

	@get:Rule
	val testHelper = MigrationTestHelper(
		instrumentation = instrumentation,
		databaseClass = RecorderDataBase::class,
		driver = AndroidSQLiteDriver(),
		file = instrumentation.targetContext.getDatabasePath(TEST_DB_NAME),
	)

	private val offsetMs: Long
		get() {
			val timeZone = TimeZone.currentSystemDefault()
			val instant = Clock.System.now()

			val utcOffset = timeZone.offsetAt(instant)
			// Convert the UtcOffset to milliseconds
			return utcOffset.totalSeconds * 1000L
		}

	// the helper refuses to create a database when the file already exists,
	// so each test starts by removing the leftovers of the previous one
	private fun deleteTestDatabase() {
		val dbFile = instrumentation.targetContext.getDatabasePath(TEST_DB_NAME)

		dbFile.parentFile?.mkdirs()

		listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
			File(dbFile.parentFile, TEST_DB_NAME + suffix).delete()
		}
	}

	// counts the number of entries from a table
	private fun SQLiteConnection.countOf(table: String): Long =
		prepare("SELECT COUNT(*) FROM $table").use { statement ->
			assertThat(statement.step()).isTrue()
			statement.getLong(0)
		}

	@Test
	fun migrate_1_to_2_creates_metadata_and_category_tables() = runTest {
		deleteTestDatabase()

		var db = testHelper.createDatabase(1).apply {
			execSQL("INSERT INTO trash_files_data_table VALUES (1, \"TRASH_ENTRY\", \"DISPLAY NAME\", \"audio/*\", 2000, 5000, \"some_location\")")
		}
		db.close()

		db = testHelper.runMigrationsAndValidate(2)

		assertThat(db.countOf("trash_files_data_table")).isEqualTo(1L)
		assertThat(db.countOf("recording_meta_data")).isEqualTo(0L)
		assertThat(db.countOf("recordings_category")).isEqualTo(0L)
		db.close()
	}

	@Test
	fun migrate_2_to_3_adds_color_and_type_columns_to_category() = runTest {
		deleteTestDatabase()
		var db = testHelper.createDatabase(2).apply {
			execSQL("INSERT INTO recordings_category VALUES (1, \"SOME NAME\", 1000)")
			execSQL("INSERT INTO recording_meta_data VALUES (1, 0, 1)")
		}
		db.close()

		db = testHelper.runMigrationsAndValidate(3)

		db.prepare("SELECT CATEGORY_NAME, CREATED_AT, COLOR, type FROM recordings_category")
			.use { statement ->
				assertThat(statement.step()).isTrue()

				assertThat(statement.getText(0)).isEqualTo("SOME NAME")
				assertThat(statement.getLong(1)).isEqualTo(1000L)

				assertThat(statement.isNull(2)).isTrue()
				assertThat(statement.isNull(3)).isTrue()
			}

		assertThat(db.countOf("recording_meta_data")).isEqualTo(1L)

		db.close()
	}

	@Test
	fun migrate_3_to_4_creates_bookmark_table_keeping_existing_rows() = runTest {
		deleteTestDatabase()

		var db = testHelper.createDatabase(3).apply {
			execSQL("INSERT INTO recordings_category VALUES (1, \"SOME NAME\", 1000, \"#FF0000\", \"SOME TYPE\")")
			execSQL("INSERT INTO recording_meta_data VALUES (1, 1, 1)")
		}
		db.close()

		db = testHelper.runMigrationsAndValidate(4)

		assertThat(db.countOf("recordings_category")).isEqualTo(1L)
		assertThat(db.countOf("recording_meta_data")).isEqualTo(1L)

		assertThat(db.countOf("recording_bookmark_table")).isEqualTo(0L)
		db.close()
	}

	@Test
	fun migrate_4_to_5_adds_recording_id_index_on_bookmarks() = runTest {
		deleteTestDatabase()

		var db = testHelper.createDatabase(4).apply {
			execSQL("INSERT INTO recording_meta_data (RECORDING_ID,IS_FAVOURITE) VALUES (1, 0)")
			execSQL("INSERT INTO recording_bookmark_table VALUES (1, \"FIRST\", 1, 1000)")
			execSQL("INSERT INTO recording_bookmark_table VALUES (2, \"SECOND\", 1, 2000)")
		}
		db.close()

		db = testHelper.runMigrationsAndValidate(5)

		assertThat(db.countOf("recording_bookmark_table")).isEqualTo(2L)

		db.prepare(
			"SELECT name FROM sqlite_master " +
					"WHERE type = 'index' AND tbl_name = 'recording_bookmark_table' " +
					"AND name = 'index_recording_bookmark_table_RECORDING_ID'"
		).use { statement -> assertThat(statement.step()).isTrue() }

		db.close()
	}

	@Test
	fun migrate_6_to_7_creates_stt_models_table_keeping_existing_rows() = runTest {
		deleteTestDatabase()

		var db = testHelper.createDatabase(6).apply {
			execSQL("INSERT INTO trash_files_data_table VALUES (1, \"TRASH_ENTRY\", \"DISPLAY NAME\", \"audio/*\", 2000, 5000, \"some_location\")")
			execSQL("INSERT INTO recordings_category VALUES (1, \"SOME NAME\", 1000, \"#FF0000\", \"SOME TYPE\")")
			execSQL("INSERT INTO recording_meta_data VALUES (1, 0, 1)")
			execSQL("INSERT INTO recording_bookmark_table VALUES (1, \"FIRST\", 1, 1000)")
		}
		db.close()

		db = testHelper.runMigrationsAndValidate(7)

		assertThat(db.countOf("trash_files_data_table")).isEqualTo(1L)
		assertThat(db.countOf("recordings_category")).isEqualTo(1L)
		assertThat(db.countOf("recording_meta_data")).isEqualTo(1L)
		assertThat(db.countOf("recording_bookmark_table")).isEqualTo(1L)

		db.execSQL("INSERT INTO stt_models_table VALUES (\"model-1\", \"en-in\", \"https://example.com/model.zip\", \"v1.0\", 1, null, 1024, 0, 12345)")

		db.prepare("SELECT _id, local_model_status, local_model_size FROM stt_models_table")
			.use { statement ->
				assertThat(statement.step()).isTrue()
				assertThat(statement.getText(0)).isEqualTo("model-1")
				assertThat(statement.getLong(1)).isEqualTo(0L)
				assertThat(statement.getLong(2)).isEqualTo(1024L)
			}

		db.close()
	}

	@Test
	fun migrate_1_to_7_end_to_end_preserves_data() = runTest {
		deleteTestDatabase()

		var db = testHelper.createDatabase(1).apply {
			execSQL("INSERT INTO trash_files_data_table VALUES (1, \"TRASH_ENTRY\", \"DISPLAY NAME\", \"audio/*\", 2000, 5000, \"some_location\")")
		}
		db.close()

		db = testHelper.runMigrationsAndValidate(7, listOf(DBMigrations.MIGRATE_5_6))

		db.prepare("SELECT DATE_ADDED, DATE_EXPIRES FROM trash_files_data_table").use { statement ->
			assertThat(statement.step()).isTrue()
			val newDateAdded = statement.getLong(0)
			val newDateExpires = statement.getLong(1)
			assertThat(newDateAdded).isEqualTo(2000 + offsetMs)
			assertThat(newDateExpires).isEqualTo(5000 + offsetMs)
		}

		assertThat(db.countOf("recording_meta_data")).isEqualTo(0L)
		assertThat(db.countOf("recordings_category")).isEqualTo(0L)
		assertThat(db.countOf("recording_bookmark_table")).isEqualTo(0L)
		assertThat(db.countOf("stt_models_table")).isEqualTo(0L)

		db.close()
	}

	@Test
	fun migrate_5_to_6_correcting_timestamp_in_bookmarks_and_category() = runTest {
		deleteTestDatabase()

		var db = testHelper.createDatabase(5).apply {
			execSQL("INSERT INTO recording_meta_data (RECORDING_ID,IS_FAVOURITE) VALUES (1, 0)")
			execSQL("INSERT INTO recording_bookmark_table (BOOKMARK_ID,BOOKMARK_TEXT,RECORDING_ID, BOOKMARK_TIMESTAMP) VALUES (1,\"RANDOM TEXT\",1, 1000)")
			execSQL("INSERT INTO recordings_category VALUES (1, \"SOME NAME\", 1000, null, null)")
		}
		db.close()

		db = testHelper.runMigrationsAndValidate(6, listOf(DBMigrations.MIGRATE_5_6))

		db.prepare("SELECT BOOKMARK_TIMESTAMP FROM recording_bookmark_table").use { statement ->
			assertThat(statement.step()).isTrue()
			val updated = statement.getLong(0)
			assertThat(updated).isEqualTo(1000 + offsetMs)
		}

		db.prepare("SELECT CREATED_AT FROM recordings_category").use { statement ->
			assertThat(statement.step()).isTrue()
			val updated = statement.getLong(0)
			assertThat(updated).isEqualTo(1000 + offsetMs)
		}

		db.close()
	}

	@Test
	fun migrate_5_to_6_correcting_trash_file_entity() = runTest {
		deleteTestDatabase()

		var db = testHelper.createDatabase(5).apply {
			execSQL("INSERT INTO trash_files_data_table  VALUES (1, \"TRASH_ENTRY\", \"DISPLAY NAME\", \"audio/*\", 2000, 5000, \"some_location\")")
		}
		db.close()

		db = testHelper.runMigrationsAndValidate(6, listOf(DBMigrations.MIGRATE_5_6))

		db.prepare("SELECT DATE_ADDED, DATE_EXPIRES FROM trash_files_data_table").use { statement ->
			assertThat(statement.step()).isTrue()
			val newDateAdded = statement.getLong(0)
			val newDateExpires = statement.getLong(1)
			assertThat(newDateAdded).isEqualTo(2000 + offsetMs)
			assertThat(newDateExpires).isEqualTo(5000 + offsetMs)
		}
		db.close()
	}
}
