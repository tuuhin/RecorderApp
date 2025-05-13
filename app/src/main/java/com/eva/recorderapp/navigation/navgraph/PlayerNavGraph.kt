package com.eva.recorderapp.navigation.navgraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import com.eva.feature_editor.audioEditorRoute
import com.eva.feature_player.audioPlayerRoute
import com.eva.ui.navigation.PlayerSubGraph

fun NavGraphBuilder.playerNavGraph(controller: NavHostController) =
	navigation<PlayerSubGraph.NavGraph>(
		startDestination = PlayerSubGraph.AudioPlayerRoute::class
	) {
		audioPlayerRoute(controller = controller)
		audioEditorRoute(controller = controller)
	}