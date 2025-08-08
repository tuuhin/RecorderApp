package com.eva.feature_onboarding.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eva.datastore.domain.repository.PreferencesSettingsRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class OnBoardingViewmodel @Inject constructor(
	private val repository: PreferencesSettingsRepo
) : ViewModel() {

	private val _state = MutableStateFlow(OnBoardingState.UNKNOWN)

	val boardingState = _state.onStart { checkState() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Eagerly,
			initialValue = OnBoardingState.UNKNOWN
		)

	private fun checkState() = repository.canShowOnBoardingScreenFlow
		.onEach { state ->
			_state.update {
				if (state) OnBoardingState.SHOW_BOARDING_PAGE
				else OnBoardingState.SHOW_CONTENT
			}
		}
		.launchIn(viewModelScope)

	fun onSetShowFalse() = viewModelScope.launch {
		repository.updateCanShowOnBoarding(false)
	}
}


