package com.eva.location.provider

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.getSystemService
import com.eva.location.domain.BaseLocationModel
import com.eva.location.domain.exceptions.CurrentLocationTimeoutException
import com.eva.location.domain.exceptions.LocationNotEnabledException
import com.eva.location.domain.exceptions.LocationPermissionNotFoundException
import com.eva.location.domain.exceptions.LocationProviderNotFoundException
import com.eva.location.domain.repository.LocationProvider
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LastLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class CoarseLocationProviderImpl(private val context: Context) : LocationProvider {

	private val locationManager by lazy { context.getSystemService<LocationManager>() }

	private val locationProvider by lazy { LocationServices.getFusedLocationProviderClient(context.applicationContext) }

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
	private suspend fun getCurrentLocation(): Result<BaseLocationModel> {
		return suspendCancellableCoroutine { cont ->
			locationProvider.getCurrentLocation(currentLocationRequest, null).apply {
				addOnCompleteListener {
					addOnSuccessListener { location ->
						if (location == null) {
							cont.resume(Result.failure(CurrentLocationTimeoutException()))
							return@addOnSuccessListener
						}
						val evaluatedLocation = BaseLocationModel(
							latitude = location.latitude,
							longitude = location.longitude
						)
						cont.resume(value = Result.success(evaluatedLocation))

					}
					addOnFailureListener { exp -> cont.resume(value = Result.failure(exp)) }
				}
				addOnCanceledListener {
					cont.cancel()
				}
			}
		}
	}

	@SuppressLint("MissingPermission")
	private suspend fun getLastKnownLocation(): Result<BaseLocationModel> {
		return suspendCancellableCoroutine { cont ->
			locationProvider.getLastLocation(lastLocationRequest).apply {
				addOnCompleteListener {
					addOnSuccessListener { location ->
						if (location == null) {
							cont.resume(Result.failure(CurrentLocationTimeoutException()))
							return@addOnSuccessListener
						}
						val evaluatedLocation = BaseLocationModel(
							latitude = location.latitude,
							longitude = location.longitude
						)
						cont.resume(value = Result.success(evaluatedLocation))

					}
					addOnFailureListener { exp -> cont.resume(value = Result.failure(exp)) }
				}
				addOnCanceledListener {
					cont.cancel()
				}
			}
		}
	}

	override suspend fun invoke(fetchCurrentIfNotFound: Boolean): Result<BaseLocationModel> {
		if (!_hasLocationPermission) return Result.failure(LocationPermissionNotFoundException())
		if (!isLocationEnabled) return Result.failure(LocationNotEnabledException())
		if (!isNetworkProviderEnabled || !isGpsEnabled)
			return Result.failure(LocationProviderNotFoundException())

		val lastLocation = getLastKnownLocation()
		lastLocation.onFailure { exp ->
			if (exp is CurrentLocationTimeoutException && fetchCurrentIfNotFound)
				getCurrentLocation()
		}
		return lastLocation
	}
}