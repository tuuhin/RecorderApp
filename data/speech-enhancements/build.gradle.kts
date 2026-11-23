plugins {
    alias(libs.plugins.recorderapp.android.library)
}

android {
    namespace = "com.eva.speech_enhancements"
    ndkVersion = libs.versions.android.ndk.get()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
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
    // core testing
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.kotlin.test.junit)
    androidTestImplementation(libs.assertk)
    // android annotations
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.media3.test.utils)
    androidTestImplementation(libs.androidx.media3.extractor)
}
