package com.eva.recorderapp.voice_recorder.data.recordings.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

@Database(
	entities = [TrashFileEntity::class],
	version = 1,
	exportSchema = true
)
@TypeConverters(
	value = [LocalDateTimeConvertors::class]
)
abstract class RecorderDataBase : RoomDatabase() {

	abstract fun trashMetadataEntityDao(): TrashFileDao

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
						.setQueryExecutor(Dispatchers.IO.asExecutor())
						.build()
				}
				instance!!
			}
		}
	}
}