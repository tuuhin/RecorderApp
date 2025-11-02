@file:OptIn(ExperimentalTime::class)

package com.eva.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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

		override fun migrate(db: SupportSQLiteDatabase) {

			db.execSQL("UPDATE recording_bookmark_table set BOOKMARK_TIMESTAMP = BOOKMARK_TIMESTAMP + $offset")
			db.execSQL("UPDATE recordings_category set CREATED_AT = CREATED_AT + $offset")
			db.execSQL("UPDATE trash_files_data_table set DATE_ADDED = DATE_ADDED + $offset , DATE_EXPIRES = DATE_EXPIRES + $offset")
		}
	}
}