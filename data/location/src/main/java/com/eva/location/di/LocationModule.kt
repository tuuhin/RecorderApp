package com.eva.location.di

import android.content.Context
import com.eva.location.domain.repository.LocationAddressProvider
import com.eva.location.domain.repository.LocationProvider
import com.eva.location.provider.CoarseLocationProviderImpl
import com.eva.location.provider.LocationAddressProviderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

	@Provides
	@Singleton
	fun providesCoarseLocationProvider(
		@ApplicationContext context: Context,
	): LocationProvider = CoarseLocationProviderImpl(context)

	@Provides
	@Singleton
	fun providesLocationAddressProvider(
		@ApplicationContext context: Context,
	): LocationAddressProvider = LocationAddressProviderImpl(context)
}