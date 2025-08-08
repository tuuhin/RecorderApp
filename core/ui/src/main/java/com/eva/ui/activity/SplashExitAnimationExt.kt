package com.eva.ui.activity

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.splashscreen.SplashScreen

fun SplashScreen.animateOnExit(
	screenViewDuration: Long = 200L,
	onAnimationStart: () -> Unit = {},
	onAnimationEnd: () -> Unit = {}
) {

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