package com.eva.recorderapp.voice_recorder.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashFilesMetaDataDao {

	@Query("SELECT * FROM trash_files_data_table")
	fun getAllTrashFilesFlow(): Flow<List<TrashFileMetaDataEntity>>

	@Query("SELECT * FROM trash_files_data_table")
	suspend fun getAllTrashFiles(): List<TrashFileMetaDataEntity>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun addNewTrashFile(entity: TrashFileMetaDataEntity): Long

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun addNewTrashFiles(entities: List<TrashFileMetaDataEntity>)

	@Query("DELETE FROM trash_files_data_table WHERE ID=:id")
	suspend fun deleteTrashEnityFromId(id: Long)

	@Delete
	suspend fun deleteTrashEntity(entity: TrashFileMetaDataEntity)
}