plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
	alias(libs.plugins.recorderapp.compose.compiler)
}

android {
	namespace = "com.eva.feature_editor"

	buildFeatures {
		compose = true
	}
}

dependencies {
	//navigation
	implementation(libs.androidx.navigation.compose)
	implementation(libs.androidx.hilt.navigation.compose)

	// core
	implementation(project(":core:ui"))
	implementation(project(":core:utils"))
	implementation(project(":data:editor"))
	implementation(project(":data:player"))
	implementation(project(":data:recordings"))
	implementation(project(":feature:player-shared"))

	// feature
}