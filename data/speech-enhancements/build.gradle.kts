plugins {
    alias(libs.plugins.recorderapp.android.library)
    alias(libs.plugins.recorderapp.hilt)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.eva.speech_enhancements"
    ndkVersion = libs.versions.android.ndk.get()

    defaultConfig {
        ndk {
            abiFilters.addAll(setOf("armeabi-v7a", "arm64-v8a"))
        }

        consumerProguardFiles("consumer-rules.keep")

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++20")
                arguments += "-DCMAKE_SYSTEM_VERSION=${libs.versions.android.minSdk.get().toInt()}"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    // model download
    implementation(ktorLibs.client.core)
    implementation(ktorLibs.client.cio)
    implementation(libs.okio)
    implementation(libs.kotlinx.serialization.json)

    // core testing
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.kotlin.test.junit)
    androidTestImplementation(libs.assertk)
    // android annotations
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.media3.test.utils)
    androidTestImplementation(libs.androidx.media3.extractor)
}
