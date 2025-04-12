plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
}

android {
	namespace = "com.eva.worker"
}

dependencies {
	// work manager
	implementation(libs.work.runtime.ktx)
	implementation(libs.androidx.hilt.work)
	//local
	implementation(project(":core:utils"))
	implementation(project(":data:recordings"))

}