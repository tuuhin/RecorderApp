package com.eva.recorderapp.voice_recorder.data.recordings.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.eva.recorderapp.voice_recorder.data.recordings.database.DataBaseConstants

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
)
data class RecordingsMetaDataEntity(

	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "RECORDING_ID")
	val recordingId: Long,

	@ColumnInfo(name = "IS_FAVOURITE")
	val isFavourite: Boolean = false,

	@ColumnInfo(name = "CATEGORY_ID", index = true)
	val categoryId: Long? = null
)