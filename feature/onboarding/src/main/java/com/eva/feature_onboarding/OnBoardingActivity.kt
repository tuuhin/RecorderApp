package com.eva.feature_onboarding

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.eva.feature_onboarding.screen.OnBoardingState
import com.eva.feature_onboarding.screen.OnBoardingViewmodel
import com.eva.feature_onboarding.screen.OnboardingScreen
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme
import com.eva.utils.IntentConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnBoardingActivity : ComponentActivity() {

	private val viewmodel by viewModels<OnBoardingViewmodel>()

	override fun onCreate(savedInstanceState: Bundle?) {

		val splash = installSplashScreen()
		splash.setKeepOnScreenCondition { viewmodel.boardingState.value == OnBoardingState.UNKNOWN }

		super.onCreate(savedInstanceState)

		// set enable edge to edge normally
		enableEdgeToEdge()

		// on splash complete again enable edge to edge
		splash.animateOnExit(onAnimationEnd = { enableEdgeToEdge() })

		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewmodel.boardingState.collect { state ->
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
				flags = Intent.FLAG_ACTIVITY_NEW_TASK
			}
			applicationContext.startActivity(intent)
			// set activity transitions
			setTransitions()
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


	private fun SplashScreen.animateOnExit(
		onAnimationStart: () -> Unit = {},
		onAnimationEnd: () -> Unit = {}
	) {
		val screenViewDuration = 200L

		setOnExitAnimationListener { screenView ->
			// do all the animation is a reverse way
			val interpolator = AccelerateDecelerateInterpolator()

			val iconScaleXAnimation = ObjectAnimator
				.ofFloat(screenView.iconView, View.SCALE_X, 1f, 0.5f)
				.apply {
					this.interpolator = interpolator
					this.duration = screenView.iconAnimationDurationMillis
				}

			val iconScaleYAnimation = ObjectAnimator
				.ofFloat(screenView.iconView, View.SCALE_Y, 1f, 0.5f)
				.apply {
					this.interpolator = interpolator
					this.duration = screenView.iconAnimationDurationMillis
				}

			val iconTranslateYAnimation = ObjectAnimator
				.ofFloat(screenView.iconView, View.TRANSLATION_Y, 0.0f, 20.0f)
				.apply {
					this.interpolator = interpolator
					this.duration = screenView.iconAnimationDurationMillis
				}

			val viewFadeAnimation = ObjectAnimator
				.ofFloat(screenView.view, View.ALPHA, 1.0f, .2f)
				.apply {
					this.interpolator = DecelerateInterpolator()
					this.duration = screenViewDuration
				}

			val viewTranslateAnimation = ObjectAnimator.ofFloat(
				screenView.view,
				View.TRANSLATION_Y,
				0f,
				screenView.view.height.toFloat()
			).apply {
				this.interpolator = AccelerateInterpolator()
				this.duration = screenViewDuration
			}

			val viewAnimatorSet = AnimatorSet().apply {
				playTogether(viewFadeAnimation, viewTranslateAnimation)
				doOnEnd {
					screenView.remove()
					onAnimationEnd()
				}
			}

			val iconAnimatorSet = AnimatorSet().apply {
				playTogether(
					iconScaleXAnimation,
					iconScaleYAnimation,
					iconTranslateYAnimation
				)
				doOnEnd { viewAnimatorSet.start() }
				doOnStart { onAnimationStart() }

			}
			iconAnimatorSet.start()
		}
	}
}