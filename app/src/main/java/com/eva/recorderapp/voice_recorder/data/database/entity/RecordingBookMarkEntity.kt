package com.eva.recorderapp.voice_recorder.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.eva.recorderapp.voice_recorder.data.database.DataBaseConstants
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
