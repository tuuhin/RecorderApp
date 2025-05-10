plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
}

android {
	namespace = "com.eva.editor"
}

dependencies {

	// media3
	implementation(libs.androidx.media3.common)
	implementation(libs.androidx.media3.exoplayer)
	implementation(libs.androidx.media3.transformer)
	implementation(libs.androidx.media3.effects)

	//local
	implementation(project(":core:utils"))
	implementation(project(":data:player"))
	implementation(project(":data:recordings"))
	implementation(project(":data:worker"))
	implementation(project(":data:datastore"))

	//test
	testImplementation(kotlin("test"))
}