plugins {
	alias(libs.plugins.recorderapp.android.library)
	alias(libs.plugins.recorderapp.hilt)
}

android {
	namespace = "com.com.visualizer"
}

dependencies {
	implementation(libs.androidx.media3.inspector)
    implementation(project(":data:recordings"))

    // test
    androidTestImplementation(libs.assertk)
}
