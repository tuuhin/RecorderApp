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
import androidx.navigation.compose.rememberNavController
import com.eva.recorderapp.navigation.AppNavHost
import com.eva.ui.theme.RecorderAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

	private lateinit var navController: NavHostController

	override fun onCreate(savedInstanceState: Bundle?) {

		// configure splash screen
		configureSplashScreen(onAnimationEnd = { enableEdgeToEdge() })

		super.onCreate(savedInstanceState)

		// if enabled edge to edge not applied after animation
		enableEdgeToEdge()

		setContent {

			navController = rememberNavController()

			RecorderAppTheme {
				Surface(color = MaterialTheme.colorScheme.background) {
					AppNavHost(navController = navController)
				}
			}
		}
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		navController.handleDeepLink(intent)
	}
}

private fun Activity.configureSplashScreen(onAnimationEnd: () -> Unit = {}) {
	val splash = installSplashScreen()

	splash.setOnExitAnimationListener { screenView ->
		// do all the animation is reverse way
		val interpolator = AccelerateDecelerateInterpolator()
		val duration = 1000L

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
			playTogether(scaleXAnimation, scaleYAnimation, translateAnimation)
			doOnEnd {
				screenView.remove()
				onAnimationEnd()
			}
		}
		animatorSet.start()
	}
}