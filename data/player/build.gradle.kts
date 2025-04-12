plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
}

android {
	namespace = "com.eva.player"
}

dependencies {
	// media3
	implementation(libs.androidx.media3.common)
	implementation(libs.androidx.media3.exoplayer)
	implementation(libs.androidx.media3.session)

	// futures to coroutine
	implementation(libs.androidx.concurrent.futures.ktx)

	//local
	implementation(project(":core:utils"))
	implementation(project(":data:recordings"))
	implementation(project(":data:datastore"))
}