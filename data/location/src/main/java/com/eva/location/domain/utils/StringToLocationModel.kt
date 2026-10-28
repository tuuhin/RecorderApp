package com.eva.location.domain.utils

import android.util.Log
import com.eva.location.domain.BaseLocationModel

private const val TAG = "LOCATION_PARSER"

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
			} ?: run {
				Log.d(TAG, "FAILED TO READ LOCATION STRING UNABLE TO PARSE THE COORDINATE")
				null
			}
		} catch (e: Exception) {
			Log.w(TAG, "FAILED TO READ LOCATION STRING", e)
			null
		}
	}
}