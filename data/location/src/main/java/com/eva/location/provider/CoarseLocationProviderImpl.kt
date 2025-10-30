package com.eva.location.provider

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
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
import com.eva.location.domain.utils.await
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LastLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException

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
			.setDurationMillis(2_000)
			.build()

	private val isGpsEnabled: Boolean
		get() = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true

	private val isNetworkProviderEnabled: Boolean
		get() = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

	override val isLocationEnabled: Boolean
		get() = locationManager?.isLocationEnabled == true

	@SuppressLint("MissingPermission")
	private suspend fun getCurrentLocation(): Result<BaseLocationModel> {
		val tokenSource = CancellationTokenSource()
		return try {
			val location = locationProvider
				.getCurrentLocation(currentLocationRequest, tokenSource.token)
				.await()
				?: return Result.failure(CurrentLocationTimeoutException())
			 Result.success(location.toDomainModel())
		} catch (e: CancellationException) {
			tokenSource.cancel()
			throw e
		} catch (e: Exception) {
			Result.failure(e)
		}
	}

	@SuppressLint("MissingPermission")
	private suspend fun getLastKnownLocation(): Result<BaseLocationModel> {
		return try {
			val location = locationProvider.getLastLocation(lastLocationRequest).await()
				?: return Result.failure(CannotFoundLastLocationException())
			Result.success(location.toDomainModel())
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			Result.failure(e)
		}
	}

	override suspend fun invoke(fetchCurrentIfNotFound: Boolean): Result<BaseLocationModel> {
		return when {
			!_hasLocationPermission -> Result.failure(LocationPermissionNotFoundException())
			!isLocationEnabled -> Result.failure(LocationNotEnabledException())
			!isNetworkProviderEnabled || !isGpsEnabled ->
				Result.failure(LocationProviderNotFoundException())

			else -> {
				val lastLocation = getLastKnownLocation()
				if (lastLocation.isSuccess) return lastLocation
				else {
					val isFetchCurrentAllowed =
						lastLocation.exceptionOrNull() is CannotFoundLastLocationException && fetchCurrentIfNotFound
					if (!isFetchCurrentAllowed) return lastLocation
					getCurrentLocation()
				}
			}
		}
	}

	private fun Location.toDomainModel() = BaseLocationModel(
		latitude = latitude,
		longitude = longitude,
		accuracy = if (hasAccuracy()) accuracy else .0f
	)
}