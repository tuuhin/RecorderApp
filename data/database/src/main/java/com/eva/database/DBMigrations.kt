package com.eva.database

import androidx.room3.DeleteColumn
import androidx.room3.migration.AutoMigrationSpec
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt

object DBMigrations {

    // Updated the timezone issue in localtime
    val MIGRATE_5_6 = object : Migration(5, 6) {

        private val offset: Long
            get() {
                val timeZone = TimeZone.currentSystemDefault()
                val instant = Clock.System.now()

                val utcOffset = timeZone.offsetAt(instant)
                // Convert the UtcOffset to milliseconds
                return utcOffset.totalSeconds * 1000L
            }

        override suspend fun migrate(connection: SQLiteConnection) {
            with(connection) {
                execSQL("UPDATE recording_bookmark_table set BOOKMARK_TIMESTAMP = BOOKMARK_TIMESTAMP + $offset")
                execSQL("UPDATE recordings_category set CREATED_AT = CREATED_AT + $offset")
                execSQL("UPDATE trash_files_data_table set DATE_ADDED = DATE_ADDED + $offset , DATE_EXPIRES = DATE_EXPIRES + $offset")
            }
        }
    }

    val MIGRATE_8_9 = object : Migration(8, 9) {
        // deletes the old data from the table
        override suspend fun migrate(connection: SQLiteConnection) {
            with(connection) {
                execSQL("DELETE FROM ${DataBaseConstants.STT_MODELS_TABLE}")
            }
        }
    }

    @DeleteColumn.Entries(
        DeleteColumn(
            tableName = DataBaseConstants.STT_MODELS_TABLE,
            columnName = "local_model_size",
        ),
    )
    class STTModelMigrationSpec : AutoMigrationSpec
}
