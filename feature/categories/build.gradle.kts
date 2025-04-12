plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
	alias(libs.plugins.recorderapp.compose.compiler)
}

android {
	namespace = "com.eva.feature_categories"

	buildFeatures {
		compose = true
	}
}


dependencies {
	//navigation
	implementation(libs.androidx.navigation.compose)
	implementation(libs.androidx.hilt.navigation.compose)
	//local
	implementation(project(":core:ui"))
	implementation(project(":core:utils"))
	implementation(project(":data:categories"))
	implementation(project(":data:recordings"))
}