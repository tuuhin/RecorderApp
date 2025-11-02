package com.eva.ui.activity

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.splashscreen.SplashScreen

private const val TAG = "SplashExitAnimation"

fun SplashScreen.animateOnExit(
	screenViewDuration: Long = 300L,
	onAnimationStart: () -> Unit = {},
	onAnimationEnd: () -> Unit = {}
) = setOnExitAnimationListener { screenView ->

	// TODO: Cannot reproduce but sometime icon view is null
	val icon = try {
		screenView.iconView
	} catch (_: NullPointerException) {
		null
	}

	val viewTranslateAnimation = ObjectAnimator
		.ofFloat(screenView.view, View.TRANSLATION_Y, 0f, screenView.view.height.toFloat())
		.setDuration(screenViewDuration)
		.apply { this.interpolator = AccelerateInterpolator() }

	val viewFadeAnimation = ObjectAnimator
		.ofFloat(screenView.view, View.ALPHA, 1.0f, .2f)
		.setDuration(screenViewDuration)
		.apply { this.interpolator = DecelerateInterpolator() }

	val viewAnimatorSet = AnimatorSet().apply {
		playTogether(viewFadeAnimation, viewTranslateAnimation)
		doOnEnd {
			screenView.remove()
			onAnimationEnd()
		}
	}

	if (icon == null) {
		Log.d(TAG, "ICON VIEW NOT FOUND FALLBACK TO VIEW ANIMATION")
		// fallback: no icon animation
		onAnimationStart()
		viewAnimatorSet.start()
		return@setOnExitAnimationListener
	}

	val iconScaleXAnimation = ObjectAnimator
		.ofFloat(icon, View.SCALE_X, 1f, 0.5f)
		.setDuration(screenView.iconAnimationDurationMillis)
		.apply { this.interpolator = interpolator }

	val iconScaleYAnimation = ObjectAnimator
		.ofFloat(icon, View.SCALE_Y, 1f, 0.5f)
		.setDuration(screenView.iconAnimationDurationMillis)
		.apply { this.interpolator = interpolator }

	val iconTranslateYAnimation = ObjectAnimator
		.ofFloat(icon, View.TRANSLATION_Y, 0.0f, 20.0f)
		.setDuration(screenView.iconAnimationDurationMillis)
		.apply { this.interpolator = interpolator }

	val iconAnimatorSet = AnimatorSet().apply {
		playTogether(
			iconScaleXAnimation,
			iconScaleYAnimation,
			iconTranslateYAnimation
		)
		doOnEnd {
			viewAnimatorSet.startDelay = 50L
			viewAnimatorSet.start()
		}
		doOnStart { onAnimationStart() }
	}
	iconAnimatorSet.start()
}