plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
	alias(libs.plugins.recorderapp.compose.compiler)
}

android {
	namespace = "com.eva.feature_settings"
	buildFeatures {
		compose = true
	}
}


dependencies {
	//navigation
	implementation(libs.androidx.navigation.compose)
	implementation(libs.androidx.hilt.navigation.compose)
	//icons
	implementation(libs.androidx.icons.extended)

	implementation(project(":core:ui"))
	implementation(project(":core:utils"))
	implementation(project(":data:recordings"))
	implementation(project(":data:datastore"))

}