package com.eva.recorderapp.voice_recorder.data.recordings.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.eva.recorderapp.voice_recorder.data.recordings.database.convertors.CategoriesEnumConvertors
import com.eva.recorderapp.voice_recorder.data.recordings.database.convertors.LocalDateTimeConvertors
import com.eva.recorderapp.voice_recorder.data.recordings.database.dao.RecordingCategoryDao
import com.eva.recorderapp.voice_recorder.data.recordings.database.dao.RecordingsMetadataDao
import com.eva.recorderapp.voice_recorder.data.recordings.database.dao.TrashFileDao
import com.eva.recorderapp.voice_recorder.data.recordings.database.entity.RecordingCategoryEntity
import com.eva.recorderapp.voice_recorder.data.recordings.database.entity.RecordingsMetaDataEntity
import com.eva.recorderapp.voice_recorder.data.recordings.database.entity.TrashFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

@Database(
	entities = [
		TrashFileEntity::class,
		RecordingsMetaDataEntity::class,
		RecordingCategoryEntity::class,
	],
	version = 3,
	exportSchema = true,
	autoMigrations = [
		AutoMigration(from = 1, to = 2),
		AutoMigration(from = 2, to = 3)
	]
)
@TypeConverters(
	value = [
		LocalDateTimeConvertors::class,
		CategoriesEnumConvertors::class
	]
)
abstract class RecorderDataBase : RoomDatabase() {

	abstract fun trashMetadataEntityDao(): TrashFileDao

	abstract fun categoriesDao(): RecordingCategoryDao

	abstract fun recordingMetaData(): RecordingsMetadataDao

	companion object {

		@Volatile
		private var instance: RecorderDataBase? = null

		fun createDataBase(context: Context): RecorderDataBase {
			return synchronized(this) {
				if (instance == null) {
					instance = Room.databaseBuilder(
						context,
						RecorderDataBase::class.java,
						DataBaseConstants.DATABASE_NAME
					)
						.addTypeConverter(LocalDateTimeConvertors())
						.addTypeConverter(CategoriesEnumConvertors())
						.setQueryExecutor(Dispatchers.IO.asExecutor())
						.build()
				}
				instance!!
			}
		}
	}
}