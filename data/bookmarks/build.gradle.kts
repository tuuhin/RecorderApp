plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
}

android {
	namespace = "com.eva.bookmarks"
}

dependencies {
	implementation(project(":core:utils"))
	implementation(project(":data:recordings"))
	implementation(project(":data:database"))
}