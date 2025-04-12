plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
}

android {
	namespace = "com.eva.recorder"
}

dependencies {

	// lifecyle service and media3 common
	implementation(libs.androidx.lifecycle.service)
	implementation(libs.androidx.media3.common)

	//local
	implementation(project(":core:utils"))
	implementation(project(":data:use_case"))
	implementation(project(":data:location"))
	implementation(project(":data:datastore"))
	implementation(project(":data:recordings"))
	implementation(project(":data:bookmarks"))
}