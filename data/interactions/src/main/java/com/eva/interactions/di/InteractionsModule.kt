package com.eva.interactions.di

import android.content.Context
import android.os.Build
import com.eva.bookmarks.domain.provider.ExportBookMarkUriProvider
import com.eva.interactions.data.AppShortcutsUtilsImpl
import com.eva.interactions.data.ShareRecordingsUtilImpl
import com.eva.interactions.data.bluetooth.BluetoothScoConnectImpl
import com.eva.interactions.data.bluetooth.BluetoothScoConnectImplApi31
import com.eva.interactions.data.phone.PhoneStateObserverImpl
import com.eva.interactions.data.phone.PhoneStateObserverImplApi31
import com.eva.interactions.domain.AppShortcutFacade
import com.eva.interactions.domain.BluetoothScoConnect
import com.eva.interactions.domain.PhoneStateObserver
import com.eva.interactions.domain.ShareRecordingsUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InteractionsModule {

	@Provides
	@Singleton
	fun providesAppShortCutUtil(@ApplicationContext context: Context)
			: AppShortcutFacade = AppShortcutsUtilsImpl(context)

	@Provides
	@Singleton
	fun providesBluetoothScoConnector(@ApplicationContext context: Context): BluetoothScoConnect {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
			BluetoothScoConnectImplApi31(context)
		else BluetoothScoConnectImpl(context)
	}

	@Provides
	@Singleton
	fun providesPhoneStateObserver(@ApplicationContext context: Context): PhoneStateObserver {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
			PhoneStateObserverImplApi31(context)
		else PhoneStateObserverImpl(context)
	}

	@Provides
	@Singleton
	fun providesShareRecordingHelper(
		@ApplicationContext context: Context,
		bookmarkProvider: ExportBookMarkUriProvider,
	): ShareRecordingsUtil = ShareRecordingsUtilImpl(context, bookmarkProvider)
}