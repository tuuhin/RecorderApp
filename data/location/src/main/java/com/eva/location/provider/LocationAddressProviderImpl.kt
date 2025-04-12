package com.eva.location.provider

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.location.domain.BaseLocationModel
import com.eva.location.domain.repository.LocationAddressProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "GEO_CODER"

internal class LocationAddressProviderImpl(
	private val context: Context,
	private val settings: RecorderAudioSettingsRepo,
) : LocationAddressProvider {

	private val geoCoder by lazy { Geocoder(context, Locale.getDefault()) }

	private val hasGeoCoder: Boolean
		get() = Geocoder.isPresent()

	private fun buildAddressFromGeoCoderAddress(address: Address) = buildString {
		val pattern = with(address) {
			listOfNotNull(locality, adminArea, countryName, postalCode)
		}

		pattern.forEachIndexed { idx, block ->
			append(block)
			if (idx + 1 != pattern.size)
				append(",")
		}
	}


	override suspend operator fun invoke(locationModel: BaseLocationModel): String? {
		if (!hasGeoCoder) {
			Log.i(TAG, "GEO CODER API IS NOT FOUND")
			return null
		}
		if (!settings.audioSettings.addLocationInfoInRecording) {
			Log.d(TAG, "NO NEED TO SHOW LOCATION ")
			return null
		}
		return withContext(Dispatchers.IO) {
			try {
				val addressPattern = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
					evalAddressAPI33(locationModel)
				else evalAddressNormal(locationModel)
				Log.d(TAG, "ADDRESS DETERMINED FROM LOCATION :$addressPattern")
				return@withContext addressPattern
			} catch (e: IOException) {
				Log.e(TAG, "LAT LONG IS INVALID", e)
				null
			} catch (e: Exception) {
				e.printStackTrace()
				null
			}
		}
	}

	@RequiresApi(Build.VERSION_CODES.TIRAMISU)
	private suspend fun evalAddressAPI33(location: BaseLocationModel) = suspendCoroutine { cont ->
		// geocoder observer
		val observer = object : Geocoder.GeocodeListener {
			override fun onGeocode(addresses: MutableList<Address>) {
				val result = addresses.firstOrNull()
					?.let(::buildAddressFromGeoCoderAddress)
				cont.resume(result)
			}

			override fun onError(errorMessage: String?) {
				Log.e(TAG, "ERROR IN USING GEOCODER :$errorMessage")
				cont.resume(null)
			}
		}
		// add the observer
		geoCoder.getFromLocation(location.latitude, location.longitude, 2, observer)
	}

	@Suppress("DEPRECATION")
	private fun evalAddressNormal(location: BaseLocationModel) =
		geoCoder.getFromLocation(location.latitude, location.longitude, 2)
			?.filterNotNull()
			?.firstOrNull()
			?.let(::buildAddressFromGeoCoderAddress)
}