plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
	alias(libs.plugins.recorderapp.protobuf)
	//not using the custom one here
	alias(libs.plugins.compose.compiler)
}

android {
	namespace = "com.eva.feature_widget"
	buildFeatures {
		compose = true
	}
}

composeCompiler {

//	featureFlags = setOf(ComposeFeatureFlag.OptimizeNonSkippingGroups)
	reportsDestination = layout.buildDirectory.dir("compose_compiler")
	metricsDestination = layout.buildDirectory.dir("compose_compiler")

	stabilityConfigurationFiles.add(
		rootProject.layout.projectDirectory.file("stability_config.conf")
	)
}

dependencies {
	//compose
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.material3)

	//glance
	implementation(libs.androidx.glance)
	implementation(libs.androidx.glance.appwidget)
	implementation(libs.androidx.glance.material3)
	implementation(libs.androidx.glance.preview)
	implementation(libs.androidx.glance.appwidget.preview)

	//core
	implementation(project(":core:utils"))
	implementation(project(":core:ui"))
	//data
	implementation(project(":data:recorder"))
	implementation(project(":data:recordings"))
	implementation(project(":data:use_case"))
}
