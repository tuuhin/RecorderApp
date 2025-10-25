plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
}

android {
	namespace = "com.eva.categories"
}

dependencies {
	implementation(project(":core:ui"))
	implementation(project(":core:utils"))
	implementation(project(":data:database"))

	androidTestImplementation(project(":testing:runtime"))
}