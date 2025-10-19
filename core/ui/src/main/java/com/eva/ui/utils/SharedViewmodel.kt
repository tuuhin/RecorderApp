package com.eva.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.eva.ui.viewmodel.AppViewModel

@Composable
inline fun <reified T : AppViewModel> NavBackStackEntry.sharedViewmodel(controller: NavController): T {

	val parent = this.destination.parent?.route ?: return hiltViewModel<T>()

	val parentBackstack = remember(this) {
		controller.getBackStackEntry(parent)
	}

	return hiltViewModel<T>(viewModelStoreOwner = parentBackstack)
}