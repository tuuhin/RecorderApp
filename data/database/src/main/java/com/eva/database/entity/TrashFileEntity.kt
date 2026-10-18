package com.eva.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.eva.database.DataBaseConstants
import kotlinx.datetime.LocalDateTime

@Entity(tableName = DataBaseConstants.TRASH_FILES_TABLE)
data class TrashFileEntity(

	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "ID")
	val id: Long,

	@ColumnInfo(name = "TITLE")
	val title: String,

	@ColumnInfo(name = "DISPLAY_NAME")
	val displayName: String,

	@ColumnInfo(name = "MIME_TYPE")
	val mimeType: String,

	@ColumnInfo(name = "DATE_ADDED")
	val dateAdded: LocalDateTime,

	@ColumnInfo(name = "DATE_EXPIRES")
	val expiresAt: LocalDateTime? = null,

	@ColumnInfo(name = "FILE")
	val file: String
)
