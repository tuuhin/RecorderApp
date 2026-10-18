package com.eva.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.eva.database.DataBaseConstants
import kotlinx.datetime.LocalTime

@Entity(
	tableName = DataBaseConstants.RECORDING_BOOKMARK_TABLE,
	foreignKeys = [
		ForeignKey(
			entity = RecordingsMetaDataEntity::class,
			childColumns = ["RECORDING_ID"],
			parentColumns = ["RECORDING_ID"],
			onUpdate = ForeignKey.CASCADE,
			onDelete = ForeignKey.CASCADE,
		),
	],
	indices = [
		Index("RECORDING_ID")
	],
)
data class RecordingBookMarkEntity(

	@PrimaryKey(autoGenerate = true)
	@ColumnInfo(name = "BOOKMARK_ID")
	val bookMarkId: Long? = null,

	@ColumnInfo(name = "BOOKMARK_TEXT")
	val text: String = "",

	@ColumnInfo(name = "RECORDING_ID")
	val recordingId: Long,

	@ColumnInfo(name = "BOOKMARK_TIMESTAMP")
	val timeStamp: LocalTime,
)
