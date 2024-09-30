package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import com.eva.recorderapp.voice_recorder.data.recorder.RecorderActionHandlerImpl
import com.eva.recorderapp.voice_recorder.data.util.AppShortcutsUtilsImpl
import com.eva.recorderapp.voice_recorder.data.util.BluetoothScoConnectImpl
import com.eva.recorderapp.voice_recorder.data.util.PhoneStateObserverImpl
import com.eva.recorderapp.voice_recorder.data.util.ShareRecordingsUtilImpl
import com.eva.recorderapp.voice_recorder.domain.bookmarks.ExportBookMarkUriProvider
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderActionHandler
import com.eva.recorderapp.voice_recorder.domain.util.AppShortcutFacade
import com.eva.recorderapp.voice_recorder.domain.util.BluetoothScoConnect
import com.eva.recorderapp.voice_recorder.domain.util.PhoneStateObserver
import com.eva.recorderapp.voice_recorder.domain.util.ShareRecordingsUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppUtilsModule {

	@Provides
	@Singleton
	fun providesRecorderActionHandler(
		@ApplicationContext context: Context
	): RecorderActionHandler = RecorderActionHandlerImpl(context)

	@Provides
	@Singleton
	fun providesShortcutFacade(
		@ApplicationContext context: Context,
	): AppShortcutFacade = AppShortcutsUtilsImpl(context)

	@Provides
	@Singleton
	fun providesBluetoothScoConnector(
		@ApplicationContext context: Context,
	): BluetoothScoConnect = BluetoothScoConnectImpl(context)

	@Provides
	@Singleton
	fun providesPhoneStateObserver(
		@ApplicationContext context: Context,
	): PhoneStateObserver = PhoneStateObserverImpl(context)


	@Provides
	@Singleton
	fun providesShareRecordingsUtils(
		@ApplicationContext context: Context,
		exportBookMarkUriProvider: ExportBookMarkUriProvider,
	): ShareRecordingsUtil = ShareRecordingsUtilImpl(context, exportBookMarkUriProvider)

}