package com.eva.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

	@Delete
	suspend fun deleteTrashEntity(entity: TrashFileEntity)
}