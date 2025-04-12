plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
}

android {
	namespace = "com.eva.interactions"
}

dependencies {
	implementation(project(":core:utils"))
	implementation(project(":data:bookmarks"))
	implementation(project(":data:recordings"))
}