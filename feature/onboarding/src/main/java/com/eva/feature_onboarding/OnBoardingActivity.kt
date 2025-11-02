package com.eva.feature_onboarding

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.eva.feature_onboarding.screen.OnBoardingState
import com.eva.feature_onboarding.screen.OnBoardingViewmodel
import com.eva.feature_onboarding.screen.OnboardingScreen
import com.eva.ui.R
import com.eva.ui.activity.animateOnExit
import com.eva.ui.theme.RecorderAppTheme
import com.eva.utils.IntentConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnBoardingActivity : ComponentActivity() {

	private val viewmodel by viewModels<OnBoardingViewmodel>()

	override fun onCreate(savedInstanceState: Bundle?) {

		val splash = installSplashScreen()
		super.onCreate(savedInstanceState)

		// set enable edge to edge normally
		enableEdgeToEdge()
		// set activity transitions
		setTransitions()

		splash.setKeepOnScreenCondition {
			val boardingState = viewmodel.boardingState.value
			boardingState == OnBoardingState.UNKNOWN ||
					boardingState == OnBoardingState.SHOW_CONTENT
		}

		// on splash complete again enable edge to edge
		splash.animateOnExit(onAnimationEnd = { enableEdgeToEdge() })

		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewmodel.boardingState.collectLatest { state ->
					if (state == OnBoardingState.SHOW_CONTENT)
						startMainActivityAndFinishCurrent()
				}
			}
		}

		setContent {
			RecorderAppTheme {
				Surface(color = MaterialTheme.colorScheme.background) {
					OnboardingScreen(onContinueToApp = viewmodel::onSetShowFalse)
				}
			}
		}
	}

	private fun startMainActivityAndFinishCurrent() {
		try {
			val intent = Intent().apply {
				setClassName(applicationContext, IntentConstants.MAIN_ACTIVITY)
				flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
			}
			startActivity(intent)
			// finish onboarding activity
			finish()
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	@Suppress("DEPRECATION")
	private fun setTransitions() {

		window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			overrideActivityTransition(
				OVERRIDE_TRANSITION_CLOSE,
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