package com.eva.recorderapp.voice_recorder.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime

@Entity(tableName = "trash_files_data_table")
data class TrashFileMetaDataEntity(

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
