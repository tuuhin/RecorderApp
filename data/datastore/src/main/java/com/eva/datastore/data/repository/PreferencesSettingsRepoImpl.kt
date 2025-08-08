package com.eva.datastore.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.eva.datastore.data.DataStoreConstants
import com.eva.datastore.domain.repository.PreferencesSettingsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal val Context.preferences by preferencesDataStore(DataStoreConstants.PREFERENCES_DATASTORE_FILE)

internal class PreferencesSettingsRepoImpl(private val context: Context) : PreferencesSettingsRepo {

	private val _preferences = booleanPreferencesKey(DataStoreConstants.SHOW_ON_BOARDING_SCREEN)

	override suspend fun canShowOnBoarding(): Boolean {
		return withContext(Dispatchers.IO) { canShowOnBoardingScreenFlow.first() }
	}

	override val canShowOnBoardingScreenFlow: Flow<Boolean>
		get() = context.preferences.data.map { prefs -> prefs[_preferences] ?: true }

	override suspend fun updateCanShowOnBoarding(canShow: Boolean) {
		context.preferences.edit { prefs ->
			prefs[_preferences] = canShow
		}
	}
}