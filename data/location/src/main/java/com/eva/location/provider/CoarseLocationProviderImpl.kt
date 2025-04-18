package com.eva.location.provider

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.getSystemService
import com.eva.location.domain.BaseLocationModel
import com.eva.location.domain.exceptions.CannotFoundLastLocationException
import com.eva.location.domain.exceptions.CurrentLocationTimeoutException
import com.eva.location.domain.exceptions.LocationNotEnabledException
import com.eva.location.domain.exceptions.LocationPermissionNotFoundException
import com.eva.location.domain.exceptions.LocationProviderNotFoundException
import com.eva.location.domain.repository.LocationProvider
import com.eva.utils.Resource
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LastLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class CoarseLocationProviderImpl(private val context: Context) : LocationProvider {

	private val locationManager by lazy { context.getSystemService<LocationManager>() }

	private val locationProvider: FusedLocationProviderClient
		get() = LocationServices.getFusedLocationProviderClient(context.applicationContext)

	private val _hasLocationPermission: Boolean
		get() = ContextCompat.checkSelfPermission(
			context,
			Manifest.permission.ACCESS_COARSE_LOCATION
		) == PermissionChecker.PERMISSION_GRANTED

	private val lastLocationRequest: LastLocationRequest
		get() = LastLocationRequest.Builder()
			.setGranularity(Granularity.GRANULARITY_COARSE)
			.setMaxUpdateAgeMillis(10_000)
			.build()

	private val currentLocationRequest: CurrentLocationRequest
		get() = CurrentLocationRequest.Builder()
			.setGranularity(Granularity.GRANULARITY_COARSE)
			.setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
			.setDurationMillis(1_000)
			.build()

	private val isGpsEnabled: Boolean
		get() = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true

	private val isLocationEnabled: Boolean
		get() = locationManager?.isLocationEnabled == true

	private val isNetworkProviderEnabled: Boolean
		get() = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

	@SuppressLint("MissingPermission")
	private suspend fun getCurrentLocation(): Resource<BaseLocationModel, Exception> {
		return suspendCancellableCoroutine { cont ->
			locationProvider.getCurrentLocation(currentLocationRequest, null).apply {
				addOnCompleteListener {
					addOnSuccessListener { location ->
						location?.let {
							val evaluatedLocation = BaseLocationModel(
								latitude = location.latitude,
								longitude = location.longitude
							)
							cont.resume(value = Resource.Success(evaluatedLocation))
							return@addOnSuccessListener
						}
						cont.resume(
							value = Resource.Error(CurrentLocationTimeoutException()),
						)
					}
					addOnFailureListener { exp ->
						cont.resume(value = Resource.Error(exp))
					}
				}
				addOnCanceledListener {
					cont.cancel()
				}
			}
		}
	}

	@SuppressLint("MissingPermission")
	private suspend fun getLastKnownLocation(): Resource<BaseLocationModel, Exception> {
		return suspendCancellableCoroutine { cont ->
			locationProvider.getLastLocation(lastLocationRequest).apply {
				addOnCompleteListener {
					addOnSuccessListener { location ->
						location?.let {
							val evaluatedLocation = BaseLocationModel(
								latitude = location.latitude,
								longitude = location.longitude
							)
							cont.resume(value = Resource.Success(evaluatedLocation))
							return@addOnSuccessListener
						}
						cont.resume(value = Resource.Error(CannotFoundLastLocationException()))
					}
					addOnFailureListener { exp ->
						cont.resume(value = Resource.Error(exp))
					}
				}
				addOnCanceledListener {
					cont.cancel()
				}
			}
		}
	}

	override suspend fun invoke(): Resource<BaseLocationModel, Exception> {
		if (!_hasLocationPermission) {
			return Resource.Error(LocationPermissionNotFoundException())
		}
		if (!isLocationEnabled) {
			return Resource.Error(LocationNotEnabledException())
		}
		if (!isNetworkProviderEnabled || !isGpsEnabled) {
			return Resource.Error(LocationProviderNotFoundException())
		}
		// has permission so gets the last known location
		return when (val lastLocation = getLastKnownLocation()) {
			is Resource.Error -> {
				// if its cannot found last location then try to get the current location
				if (lastLocation.error is CannotFoundLastLocationException)
					getCurrentLocation()
				else lastLocation
			}

			else -> lastLocation
		}

	}
}