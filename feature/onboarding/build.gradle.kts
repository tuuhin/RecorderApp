plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
	alias(libs.plugins.recorderapp.compose.compiler)
}

android {
	namespace = "com.eva.feature_onboarding"
	buildFeatures {
		compose = true
	}
}

dependencies {
	// splash api
	implementation(libs.androidx.core.splashscreen)
	//core
	implementation(project(":core:ui"))
	implementation(project(":core:utils"))
	// data
	implementation(project(":data:datastore"))
}