package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import com.eva.recorderapp.voice_recorder.data.recorder.RecorderActionHandlerImpl
import com.eva.recorderapp.voice_recorder.data.interactions.AppShortcutsUtilsImpl
import com.eva.recorderapp.voice_recorder.data.interactions.BluetoothScoConnectImpl
import com.eva.recorderapp.voice_recorder.data.interactions.PhoneStateObserverImpl
import com.eva.recorderapp.voice_recorder.data.interactions.ShareRecordingsUtilImpl
import com.eva.recorderapp.voice_recorder.domain.bookmarks.ExportBookMarkUriProvider
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderActionHandler
import com.eva.recorderapp.voice_recorder.domain.interactions.AppShortcutFacade
import com.eva.recorderapp.voice_recorder.domain.interactions.AppWidgetsRepository
import com.eva.recorderapp.voice_recorder.domain.interactions.BluetoothScoConnect
import com.eva.recorderapp.voice_recorder.domain.interactions.PhoneStateObserver
import com.eva.recorderapp.voice_recorder.domain.interactions.ShareRecordingsUtil
import com.eva.recorderapp.voice_recorder.widgets.data.AppWidgetsRepoImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppInteractionModule {

	@Provides
	@Singleton
	fun providesRecorderActionHandler(
		@ApplicationContext context: Context,
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

	@Provides
	@Singleton
	fun providesWidgetUtils(@ApplicationContext context: Context): AppWidgetsRepository =
		AppWidgetsRepoImpl(context)

}