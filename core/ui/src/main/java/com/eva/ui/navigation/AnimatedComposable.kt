package com.eva.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import kotlin.reflect.KType

inline fun <reified T : Any> NavGraphBuilder.animatedComposable(
	typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
	deepLinks: List<NavDeepLink> = emptyList(),
	noinline sizeTransform: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards SizeTransform?)? = {
		SizeTransform(clip = false) { _, _ -> spring() }
	},
	noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable<T>(
	typeMap = typeMap,
	deepLinks = deepLinks,
	enterTransition = { slideIntoContainerAndFadeIn },
	exitTransition = { slideOutOfContainerAndFadeOut },
	popEnterTransition = { slideIntoContainerAndFadeIn },
	popExitTransition = { slideOutOfContainerAndFadeOut },
	sizeTransform = sizeTransform,
	content = content
)

val AnimatedContentTransitionScope<NavBackStackEntry>.slideIntoContainerAndFadeIn: EnterTransition
	get() = slideIntoContainer(
		AnimatedContentTransitionScope.SlideDirection.Up,
		animationSpec = tween(durationMillis = 300, easing = EaseInCubic)
	) + fadeIn(animationSpec = tween(easing = EaseIn, durationMillis = 300))


val AnimatedContentTransitionScope<NavBackStackEntry>.slideOutOfContainerAndFadeOut: ExitTransition
	get() = slideOutOfContainer(
		AnimatedContentTransitionScope.SlideDirection.Up,
		animationSpec = tween(durationMillis = 300, easing = EaseOutCubic)
	) + fadeOut(animationSpec = tween(easing = EaseOut, durationMillis = 300))

