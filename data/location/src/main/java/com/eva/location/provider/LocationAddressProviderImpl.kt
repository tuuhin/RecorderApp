package com.eva.location.provider

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.eva.location.domain.BaseLocationModel
import com.eva.location.domain.exceptions.GeoCoderMissingException
import com.eva.location.domain.exceptions.InvalidLocationException
import com.eva.location.domain.repository.LocationAddressProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "GEO_CODER"

internal class LocationAddressProviderImpl(private val context: Context) :
	LocationAddressProvider {

	private val geoCoder by lazy { Geocoder(context, Locale.getDefault()) }

	private val hasGeoCoder: Boolean
		get() = Geocoder.isPresent()

	override suspend operator fun invoke(locationModel: BaseLocationModel): Result<String> {
		if (!hasGeoCoder) return Result.failure(GeoCoderMissingException())

		return withContext(Dispatchers.IO) {
			try {
				val addressPattern = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
					evalAddressAPI33(locationModel)
				else evalAddressNormal(locationModel)
				Log.d(TAG, "ADDRESS DETERMINED FROM LOCATION :$addressPattern")
				Result.success(addressPattern ?: "")
			} catch (e: IllegalArgumentException) {
				Log.e(TAG, "LAT LONG IS INVALID", e)
				Result.failure(InvalidLocationException())
			} catch (e: Exception) {
				if (e is CancellationException) throw e
				e.printStackTrace()
				Result.failure(e)
			}
		}
	}

	@RequiresApi(Build.VERSION_CODES.TIRAMISU)
	private suspend fun evalAddressAPI33(location: BaseLocationModel) = suspendCoroutine { cont ->
		// geocoder observer
		val observer = object : Geocoder.GeocodeListener {
			override fun onGeocode(addresses: MutableList<Address>) {
				val result = addresses.firstOrNull()?.buildAddressString()
				cont.resume(result)
			}

			override fun onError(errorMessage: String?) {
				Log.e(TAG, "ERROR IN USING GEOCODER :$errorMessage")
				cont.resumeWithException(Exception(errorMessage))
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
			?.buildAddressString()

	private fun Address.buildAddressString() = buildString {
		val pattern = listOfNotNull(locality, adminArea, countryName, postalCode)
		pattern.forEachIndexed { idx, block ->
			append(block)
			if (idx + 1 != pattern.size) append(", ")
		}
	}
}