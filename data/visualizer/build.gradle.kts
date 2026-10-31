plugins {
    alias(libs.plugins.recorderapp.android.library)
    alias(libs.plugins.recorderapp.hilt)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.com.visualizer"

}

kotlin {
    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

dependencies {
    // media3 for extractions
    implementation(libs.androidx.media3.inspector)

    // local modules
    implementation(project(":data:recordings"))
    implementation(project(":data:compression"))

    // cache file serialization
    implementation(libs.okio)
    implementation(libs.kotlinx.serialization.protobuf)

    // test
    androidTestImplementation(libs.assertk)
}
