package com.eva.recorderapp.voice_recorder.data.recordings.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.eva.recorderapp.voice_recorder.data.recordings.database.entity.RecordingsMetaDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingsMetadataDao {

	@Upsert
	suspend fun updateOrInsertRecordingMetadata(entity: RecordingsMetaDataEntity): Long

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun addRecordingMetaDataBulk(entities: Collection<RecordingsMetaDataEntity>)

	@Query("SELECT * FROM RECORDING_META_DATA")
	fun getAllRecordingsMetaDataAsFlow(): Flow<List<RecordingsMetaDataEntity>>

	@Query("SELECT * FROM RECORDING_META_DATA")
	suspend fun getAllRecordingsMetaDataAsList(): List<RecordingsMetaDataEntity>

	@Query("SELECT * from recording_meta_data WHERE RECORDING_ID=:id")
	suspend fun getRecordingMetaDataFromId(id: Long): RecordingsMetaDataEntity?

	@Query(
		"""
		SELECT RECORDING_META_DATA.* FROM RECORDING_META_DATA   
		INNER JOIN RECORDINGS_CATEGORY
		ON RECORDINGS_CATEGORY.CATEGORY_ID = RECORDING_META_DATA.CATEGORY_ID
		WHERE RECORDINGS_CATEGORY.CATEGORY_ID=:categoryId
		GROUP BY RECORDINGS_CATEGORY.CATEGORY_ID
		"""
	)
	fun getRecordingsFromCategoryIdAsFlow(categoryId: Long): Flow<List<RecordingsMetaDataEntity>>

	@Delete
	suspend fun deleteRecordingsMetaDataBulk(entities: Collection<RecordingsMetaDataEntity>)

	@Delete
	suspend fun deleteRecordingMetadata(entity: RecordingsMetaDataEntity)

	@Query("DELETE FROM recording_meta_data WHERE RECORDING_ID IN ( :recordingIds )")
	suspend fun deleteRecordingMetaDataFromIds(recordingIds: List<Long>)
}