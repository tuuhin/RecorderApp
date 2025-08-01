package com.eva.recorderapp

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import com.eva.recorderapp.navigation.AppNavHost
import com.eva.ui.theme.RecorderAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

	private var navController: NavHostController? = null

	override fun onCreate(savedInstanceState: Bundle?) {

		val splash = installSplashScreen()

		super.onCreate(savedInstanceState)

		// set enable edge to edge normally
		enableEdgeToEdge()
		// on splash complete again enable edge to edge
		splash.animateOnExit(onAnimationEnd = { enableEdgeToEdge() })


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