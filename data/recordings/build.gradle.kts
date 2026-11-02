plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
}

android {
	namespace = "com.eva.recordings"

	buildFeatures {
		buildConfig = true
	}
}

dependencies {
	// activity
	implementation(libs.androidx.activity.compose)
	// audio tagger to set metadata
	implementation(libs.kotlinx.io.core)
	implementation(libs.jaudiotagger.kmp)
	//local
	implementation(project(":core:utils"))
	implementation(project(":data:database"))
	implementation(project(":data:datastore"))
	implementation(project(":data:location"))
	implementation(project(":data:categories"))
}