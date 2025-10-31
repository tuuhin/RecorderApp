package com.eva.datastore.di

import android.content.Context
import com.eva.datastore.data.DefaultDataStoreProvider
import com.eva.datastore.domain.DataStoreProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatastoreModule {

	@Provides
	@Singleton
	fun providesDatastoreInstance(@ApplicationContext context: Context): DataStoreProvider =
		DefaultDataStoreProvider(context)
}