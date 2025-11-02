package com.eva.datastore.di

import android.content.Context
import com.eva.datastore.data.TestDatastoreProvider
import com.eva.datastore.domain.DataStoreProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
	components = [SingletonComponent::class],
	replaces = [DatastoreModule::class]
)
object DatastoreTestModule {

	@Singleton
	@Provides
	fun providesTestProvider(
		@ApplicationContext context: Context
	): DataStoreProvider = TestDatastoreProvider(context)
}