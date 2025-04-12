plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
}

android {
	namespace = "com.eva.recordings"
}

dependencies {
	// activity
	implementation(libs.androidx.activity.compose)
	//local
	implementation(project(":core:utils"))
	implementation(project(":data:database"))
	implementation(project(":data:datastore"))
	implementation(project(":data:location"))
	implementation(project(":data:categories"))
}