package com.eva.recorderapp

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import com.eva.recorderapp.navigation.AppNavHost
import com.eva.ui.theme.RecorderAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

	private var navController: NavHostController? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// configure splash screen
		configureSplashScreen(onAnimationEnd = { enableEdgeToEdge() })
		// if enabled edge to edge not applied after animation
		enableEdgeToEdge()

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

private fun Activity.configureSplashScreen(onAnimationEnd: () -> Unit = {}) {
	val splash = installSplashScreen()

	splash.setOnExitAnimationListener { screenView ->
		// do all the animation is a reverse way
		val interpolator = AccelerateDecelerateInterpolator()
		val duration = 800L

		val scaleXAnimation = ObjectAnimator
			.ofFloat(screenView.iconView, View.SCALE_X, 1f, 0.5f)
			.apply {
				this.interpolator = interpolator
				this.duration = duration
			}

		val scaleYAnimation = ObjectAnimator
			.ofFloat(screenView.iconView, View.SCALE_Y, 1f, 0.5f)
			.apply {
				this.interpolator = interpolator
				this.duration = duration
			}

		val translateAnimation = ObjectAnimator
			.ofFloat(screenView.iconView, View.TRANSLATION_Y, 0.0f, 20.0f)
			.apply {
				this.interpolator = interpolator
				this.duration = duration
			}

		val animatorSet = AnimatorSet().apply {
			val items =
				arrayOf(scaleXAnimation, scaleYAnimation, translateAnimation).filterNotNull()
			playTogether(items)
			doOnEnd {
				screenView.remove()
				onAnimationEnd()
			}
		}
		animatorSet.start()
	}
}