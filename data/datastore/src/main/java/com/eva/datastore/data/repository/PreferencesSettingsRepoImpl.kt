package com.eva.datastore.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.eva.datastore.data.DataStoreConstants
import com.eva.datastore.domain.repository.PreferencesSettingsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class PreferencesSettingsRepoImpl(
	private val preferences: DataStore<Preferences>
) : PreferencesSettingsRepo {

	private val _preferences = booleanPreferencesKey(DataStoreConstants.SHOW_ON_BOARDING_SCREEN)

	override suspend fun canShowOnBoarding(): Boolean {
		return withContext(Dispatchers.IO) { canShowOnBoardingScreenFlow.first() }
	}

	override val canShowOnBoardingScreenFlow: Flow<Boolean>
		get() = preferences.data.map { prefs -> prefs[_preferences] ?: true }

	override suspend fun updateCanShowOnBoarding(canShow: Boolean) {
		preferences.edit { prefs -> prefs[_preferences] = canShow }
	}
}