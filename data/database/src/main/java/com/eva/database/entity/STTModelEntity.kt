package com.eva.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eva.database.DataBaseConstants
import kotlinx.datetime.LocalDateTime

@Entity(tableName = DataBaseConstants.STT_MODELS_TABLE)
data class STTModelEntity(

	@PrimaryKey(autoGenerate = false)
	@ColumnInfo(name = "_id")
	val modelId: String,

	@ColumnInfo(name = "local_model_locale")
	val locale: String,

	@ColumnInfo(name = "model_download_uri")
	val externalModelURI: String,

	@ColumnInfo(name = "model_download_version")
	val externalModelVersion: String,

	@ColumnInfo(name = "local_metadata_version")
	val version: Int = 1,

	@ColumnInfo(name = "local_saved_model_uri")
	val modelURI: String? = null,

	@ColumnInfo(name = "local_model_size")
	val modelSize: Long = 0,

	@ColumnInfo(name = "local_model_status")
	val status: ModelStatus = ModelStatus.NOT_READY,

	@ColumnInfo(name = "updated_at")
	val updatedAt: LocalDateTime
) {

	enum class ModelStatus(val flag: Int) {
		NOT_READY(0),
		DOWNLOADING(1),
		DOWNLOADED(2),
		UPDATE_AVAILABLE(3)
	}
}
