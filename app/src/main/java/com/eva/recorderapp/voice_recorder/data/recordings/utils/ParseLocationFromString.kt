package com.eva.recorderapp.voice_recorder.data.recordings.utils

import com.eva.recorderapp.voice_recorder.domain.location.BaseLocationModel

private val latLongPattern = Regex("([+-]\\d{2}\\.\\d+)([+-]\\d{3}\\.\\d+)")

fun parseLocationFromString(locationString: String?): BaseLocationModel? {
	return locationString?.let { coordinate ->
		try {
			latLongPattern.find(coordinate)?.let { result ->
				if (result.groupValues.size < 2) return null
				// convert the grouped values to double
				val lat = result.groupValues[1].toDouble()
				val long = result.groupValues[2].toDouble()
				BaseLocationModel(lat, long)
			}
		} catch (e: Exception) {
			null
		}
	}
}