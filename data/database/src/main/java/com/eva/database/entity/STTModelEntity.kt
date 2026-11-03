package com.eva.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.eva.database.DataBaseConstants
import kotlinx.datetime.LocalDateTime

@Entity(tableName = DataBaseConstants.STT_MODELS_TABLE)
data class STTModelEntity(

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "_id")
    val modelId: String,

    @ColumnInfo(name = "model_name", defaultValue = "")
    val name: String = "",

    @ColumnInfo(name = "model_display_name", defaultValue = "")
    val displayName: String = "",

    @ColumnInfo(name = "local_model_locale")
    val locale: String,

    @ColumnInfo(name = "model_engine", defaultValue = "'vosk'")
    val engine: String = "vosk",

    @ColumnInfo(name = "model_download_uri")
    val externalModelURI: String,

    @ColumnInfo(name = "model_download_version")
    val externalModelVersion: String,

    @ColumnInfo(name = "model_archive_size", defaultValue = "0")
    val archiveSize: Long = 0L,

    @ColumnInfo(name = "model_archive_format", defaultValue = "'zip'")
    val archiveFormat: String = "zip",

    @ColumnInfo(name = "model_checksum_sha256")
    val checksum: String? = null,

    @ColumnInfo(name = "model_license", defaultValue = "''")
    val license: String = "",

    // Local installation metadata
    @ColumnInfo(name = "local_metadata_version")
    val version: Int = 1,

    @ColumnInfo(name = "local_saved_model_uri")
    val modelURI: String? = null,

    @ColumnInfo(name = "local_model_status")
    val status: ModelStatus = ModelStatus.NOT_READY,

    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime,
) {

    enum class ModelStatus(val flag: Int) {
        NOT_READY(0),
        DOWNLOADING(1),
        DOWNLOADED(2),
        UPDATE_AVAILABLE(3)
    }
}
