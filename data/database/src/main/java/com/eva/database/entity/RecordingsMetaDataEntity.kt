package com.eva.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.eva.database.DataBaseConstants

@Entity(
	tableName = DataBaseConstants.RECORDING_METADATA_TABLE,
	foreignKeys = [
		ForeignKey(
			entity = RecordingCategoryEntity::class,
			childColumns = ["CATEGORY_ID"],
			parentColumns = ["CATEGORY_ID"],
			onUpdate = ForeignKey.CASCADE,
			onDelete = ForeignKey.SET_NULL,
		),
	],
	indices = [
		Index("CATEGORY_ID")
	],
)
data class RecordingsMetaDataEntity(

	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "RECORDING_ID")
	val recordingId: Long,

	@ColumnInfo(name = "IS_FAVOURITE")
	val isFavourite: Boolean = false,

	@ColumnInfo(name = "CATEGORY_ID")
	val categoryId: Long? = null,
)