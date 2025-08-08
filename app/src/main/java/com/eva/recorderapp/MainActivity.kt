package com.eva.recorderapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import com.eva.recorderapp.navigation.AppNavHost
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

	private var navController: NavHostController? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		// splash needs to be initiated here
		installSplashScreen()

		super.onCreate(savedInstanceState)

		// set enable edge to edge normally
		enableEdgeToEdge()
		// set activity transitions
		setTransitions()

		setContent {
			RecorderAppTheme {
				Surface(color = MaterialTheme.colorScheme.background) {
					AppNavHost(
						onSetController = { controller ->
							if (navController == null) navController = controller
						},
					)
				}
			}
		}
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		navController?.handleDeepLink(intent)
	}

	@Suppress("DEPRECATION")
	private fun setTransitions() {
		// allow activity transitions
		window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
		// set transitions
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			overrideActivityTransition(
				OVERRIDE_TRANSITION_OPEN,
				R.anim.activity_enter_transition,
				R.anim.activity_exit_transition
			)
		} else {
			overridePendingTransition(
				R.anim.activity_enter_transition,
				R.anim.activity_exit_transition
			)
		}
	}
}