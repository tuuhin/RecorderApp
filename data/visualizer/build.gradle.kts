plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
}

android {
	namespace = "com.com.visualizer"
}

dependencies {
	implementation(project(":data:recordings"))
}