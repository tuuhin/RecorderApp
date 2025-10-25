plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
	alias(libs.plugins.recorderapp.room)
}
android {
	namespace = "com.eva.database"
}

dependencies {
	implementation(project(":core:utils"))
	androidTestImplementation(project(":testing:runtime"))
}