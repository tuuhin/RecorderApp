plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
	alias(libs.plugins.recorderapp.compose.compiler)

	alias(libs.plugins.kotlinx.serialization)
}

android {
	namespace = "com.eva.ui"
	buildFeatures {
		compose = true
	}
}

dependencies {
	// navigation
	implementation(libs.androidx.navigation.compose)
	implementation(libs.kotlinx.serialization.json)
	implementation(libs.androidx.core.splashscreen)

	// hilt viewmodel
	implementation(libs.androidx.hilt.navigation.compose)

	//dynamic font
	implementation(libs.androidx.ui.text.google.fonts)

	//commons
	api(libs.kotlinx.collections.immutable)
	api(libs.androidx.graphics.shapes)
	//icons
	api(libs.androidx.icons.extended)
}