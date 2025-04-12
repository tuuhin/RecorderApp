plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
}

android {
	namespace = "com.eva.location"
}

dependencies {
	// location
	implementation(libs.gms.play.services.location)
	//local
	implementation(project(":core:utils"))
	implementation(project(":data:datastore"))
}