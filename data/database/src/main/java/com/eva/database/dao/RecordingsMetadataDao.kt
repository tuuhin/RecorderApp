package com.eva.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.eva.database.entity.RecordingsMetaDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingsMetadataDao {

	@Upsert
	suspend fun updateOrInsertRecordingMetadata(entity: RecordingsMetaDataEntity): Long

	@Upsert
	suspend fun updateOrInsertRecordingMetadataBulk(entities: Collection<RecordingsMetaDataEntity>)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun addRecordingMetaDataBulk(entities: Collection<RecordingsMetaDataEntity>)

	@Query("SELECT * FROM RECORDING_META_DATA")
	fun getAllRecordingsMetaDataAsFlow(): Flow<List<RecordingsMetaDataEntity>>

	@Query("SELECT * FROM RECORDING_META_DATA")
	suspend fun getAllRecordingsMetaDataAsList(): List<RecordingsMetaDataEntity>

	@Query("SELECT * from recording_meta_data WHERE RECORDING_ID=:id")
	suspend fun getRecordingMetaDataFromId(id: Long): RecordingsMetaDataEntity?

	@Query("SELECT * from recording_meta_data WHERE RECORDING_ID=:id")
	fun getRecordingMetaDataFromIdAsFlow(id: Long): Flow<RecordingsMetaDataEntity?>

	@Query("SELECT * FROM RECORDING_META_DATA WHERE RECORDING_ID in (:recordingIds) ")
	suspend fun getRecordingMetaDataFromIds(recordingIds: List<Long>): List<RecordingsMetaDataEntity>

	@Query("SELECT * FROM RECORDING_META_DATA WHERE CATEGORY_ID=:categoryId")
	fun getRecordingsFromCategoryIdAsFlow(categoryId: Long): Flow<List<RecordingsMetaDataEntity>>

	@Query("SELECT EXISTS(SELECT 1 FROM RECORDING_META_DATA WHERE RECORDING_ID =:recordingId LIMIT 1)")
	fun checkRecordingWithIdExists(recordingId: Long): Int

	@Delete
	suspend fun deleteRecordingsMetaDataBulk(entities: Collection<RecordingsMetaDataEntity>)

	@Delete
	suspend fun deleteRecordingMetadata(entity: RecordingsMetaDataEntity)

	@Query("DELETE FROM recording_meta_data WHERE RECORDING_ID IN ( :recordingIds )")
	suspend fun deleteRecordingMetaDataFromIds(recordingIds: List<Long>)
}