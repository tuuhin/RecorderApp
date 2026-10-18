package com.eva.database.convertors

import androidx.room3.ColumnTypeConverter
import androidx.room3.ColumnTypeConverters
import androidx.room3.ProvidedColumnTypeConverter
import com.eva.database.entity.STTModelEntity

@ProvidedColumnTypeConverter
class STTModelConvertors {

	@ColumnTypeConverter
	fun fromModelStatus(state: Int): STTModelEntity.ModelStatus {
		return STTModelEntity.ModelStatus.entries.find { it.flag == state }
			?: STTModelEntity.ModelStatus.NOT_READY
	}

	@ColumnTypeConverter
	fun toModelStatus(value: STTModelEntity.ModelStatus): Int = value.flag
}