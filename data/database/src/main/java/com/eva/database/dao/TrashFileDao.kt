package com.eva.database.dao

import androidx.annotation.VisibleForTesting
import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.eva.database.entity.TrashFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashFileDao {

	@Query("SELECT * FROM trash_files_data_table")
	fun getAllTrashFilesFlow(): Flow<List<TrashFileEntity>>

	@Query("SELECT * FROM trash_files_data_table")
	suspend fun getAllTrashFiles(): List<TrashFileEntity>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun addNewTrashFile(entity: TrashFileEntity): Long

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun addNewTrashFiles(entities: List<TrashFileEntity>)

	@Query("DELETE FROM trash_files_data_table WHERE ID=:id")
	suspend fun deleteTrashEntityFromId(id: Long)

	@VisibleForTesting
	@Query("DELETE FROM trash_files_data_table")
	suspend fun deleteAllTrashEntries()

	@Delete
	suspend fun deleteTrashEntity(entity: TrashFileEntity)
}