package com.eva.recorderapp.voice_recorder.data.recordings.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eva.recorderapp.voice_recorder.data.recordings.database.entity.RecordingsMetaDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingsMetadataDao {

	@Query("SELECT * FROM recording_meta_data")
	fun getAllRecordingsAsFlow(): Flow<List<RecordingsMetaDataEntity>>

	@Query("SELECT * FROM recording_meta_data")
	suspend fun getAllRecordingsAsList(): List<RecordingsMetaDataEntity>

	@Insert(onConflict = OnConflictStrategy.ABORT)
	suspend fun addRecordingMetadata(entity: RecordingsMetaDataEntity): Long

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun addRecordingsMetaData(entities: Collection<RecordingsMetaDataEntity>)

	@Delete
	suspend fun deleteRecordingMetadata(entity: RecordingsMetaDataEntity)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun deleteRecordingsMetaData(entities: Collection<RecordingsMetaDataEntity>)
}