package com.eva.recorderapp.voice_recorder.presentation.navigation.util

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SizeTransform
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
	noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) = composable<T>(
	typeMap = typeMap,
	deepLinks = deepLinks,
	enterTransition = { slideUpAndFadeIn },
	exitTransition = { slideDownAndFadeOut },
	popEnterTransition = { slideUpAndFadeIn },
	popExitTransition = { slideDownAndFadeOut },
	sizeTransform = { SizeTransform(clip = false) },
	content = content
)

