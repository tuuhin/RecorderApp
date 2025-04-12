import dev.iurysouza.modulegraph.ModuleType
import dev.iurysouza.modulegraph.Orientation
import dev.iurysouza.modulegraph.Theme

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
	alias(libs.plugins.android.application) apply false
	alias(libs.plugins.jetbrains.kotlin.android) apply false
	alias(libs.plugins.jetbrains.kotlin.jvm) apply false
	alias(libs.plugins.compose.compiler) apply false
	alias(libs.plugins.ksp) apply false
	alias(libs.plugins.hilt) apply false
	alias(libs.plugins.androidx.room) apply false
	alias(libs.plugins.google.protobuf) apply false
	alias(libs.plugins.android.library) apply false
	id("dev.iurysouza.modulegraph") version "0.12.0"
}

moduleGraphConfig {
	readmePath = "${rootDir}/module_graph.md"
	setStyleByModuleType = true
	showFullPath = true
	nestingEnabled = true
	rootModulesRegex = ":app"
	orientation = Orientation.TOP_TO_BOTTOM
	theme = Theme.BASE(
		moduleTypes = listOf(
			ModuleType.AndroidApp("#98FB98"),
			ModuleType.AndroidLibrary("#4169E1"),
			ModuleType.Kotlin("#720e9e")
		),
	)
}