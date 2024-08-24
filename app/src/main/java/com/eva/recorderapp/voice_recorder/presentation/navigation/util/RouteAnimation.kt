package com.eva.recorderapp.voice_recorder.presentation.navigation.util

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry


val AnimatedContentTransitionScope<NavBackStackEntry>.slideUpAndFadeIn: EnterTransition
	get() = slideIntoContainer(
		AnimatedContentTransitionScope.SlideDirection.Up,
		animationSpec = tween(easing = EaseInOut)
	) + fadeIn(animationSpec = tween(easing = EaseInOut))


val AnimatedContentTransitionScope<NavBackStackEntry>.slideDownAndFadeOut: ExitTransition
	get() = slideOutOfContainer(
		AnimatedContentTransitionScope.SlideDirection.Down,
		animationSpec = tween(easing = EaseOutBounce)
	) + fadeOut(animationSpec = tween(easing = EaseOutBounce))

