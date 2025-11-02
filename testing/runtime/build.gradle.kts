import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.android.library)
	alias(libs.plugins.jetbrains.kotlin.android)
	alias(libs.plugins.hilt)
	alias(libs.plugins.ksp)
}

android {
	namespace = "com.eva.testing.runtime"

	compileSdk = libs.versions.compileSdk.getOrNull()?.toInt() ?: 36
	defaultConfig {
		minSdk = libs.versions.minSdk.getOrNull()?.toInt() ?: 29

		testInstrumentationRunner = "com.eva.testing.runtime.HiltTestRunner"
		consumerProguardFiles("consumer-rules.pro")
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_17
	}
}

dependencies {
	implementation(libs.hilt.android)
	implementation(libs.hilt.test)
	implementation(libs.androidx.runner)
	ksp(libs.hilt.android.compiler)
}