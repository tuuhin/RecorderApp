plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
	alias(libs.plugins.recorderapp.protobuf)
}

android {
	namespace = "com.eva.datastore"
}

dependencies {
	//datastore
	implementation(libs.androidx.datastore)
	implementation(libs.androidx.datastore.preferences)
	//local
	implementation(project(":core:utils"))
}