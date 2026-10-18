package com.eva.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.eva.database.DataBaseConstants
import kotlinx.datetime.LocalDateTime

@Entity(
	tableName = DataBaseConstants.RECORDING_CATEGORY_TABLE,
	indices = [
		Index(value = arrayOf("CATEGORY_NAME"), unique = true)
	]
)
data class RecordingCategoryEntity(

	@PrimaryKey(autoGenerate = true)
	@ColumnInfo(name = "CATEGORY_ID")
	val categoryId: Long? = null,

	@ColumnInfo(name = "CATEGORY_NAME")
	val categoryName: String,

	@ColumnInfo(name = "CREATED_AT")
	val createdAt: LocalDateTime,

	@ColumnInfo(name = "COLOR")
	val color: String? = null,

	@ColumnInfo(name = "type")
	val type: String? = null,
)