package com.eva.datastore.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesSettingsRepo {

	suspend fun canShowOnBoarding(): Boolean

	val canShowOnBoardingScreenFlow: Flow<Boolean>

	suspend fun updateCanShowOnBoarding(canShow: Boolean)
}