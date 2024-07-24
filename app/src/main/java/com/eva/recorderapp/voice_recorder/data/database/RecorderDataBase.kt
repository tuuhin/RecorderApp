package com.eva.recorderapp.voice_recorder.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

@Database(
	entities = [TrashFileMetaDataEntity::class],
	version = 1,
	exportSchema = true
)
@TypeConverters(
	value = [LocalDateTimeConvertors::class]
)
abstract class RecorderDataBase : RoomDatabase() {

	abstract fun trashMetadataEntityDao(): TrashFilesMetaDataDao

	companion object {

		private const val DATABASE_NAME = "app_database"

		@Volatile
		private var instance: RecorderDataBase? = null

		fun createDataBase(context: Context): RecorderDataBase {
			return synchronized(this) {
				if (instance == null) {
					instance = Room.databaseBuilder(context, RecorderDataBase::class.java, DATABASE_NAME)
						.addTypeConverter(LocalDateTimeConvertors())
						.setQueryExecutor(Dispatchers.IO.asExecutor())
						.build()
				}
				instance!!
			}
		}
	}
}