plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.jetbrains.kotlin.android)
	alias(libs.plugins.compose.compiler)
	alias(libs.plugins.ksp)
	alias(libs.plugins.hilt)
	alias(libs.plugins.kotlinx.serialization)
}

android {
	namespace = "com.eva.recorderapp"
	compileSdk = 34

	defaultConfig {
		applicationId = "com.eva.recorderapp"
		minSdk = 28
		targetSdk = 34
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		vectorDrawables {
			useSupportLibrary = true
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			applicationIdSuffix = ".release"
			isShrinkResources = true
			multiDexEnabled = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
	kotlinOptions {
		jvmTarget = "1.8"
	}
	buildFeatures {
		compose = true
	}
	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
}

composeCompiler {
	//	enableStrongSkippingMode = true

	reportsDestination = layout.buildDirectory.dir("compose_compiler")
	stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
}

dependencies {
	//android
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.activity.compose)
	//compose
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.ui)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.ui.tooling.preview)
	implementation(libs.androidx.material3)
	//icons
	implementation(libs.material.icons.extended)
	//navigation
	implementation(libs.androidx.navigation.compose)
	implementation(libs.androidx.hilt.navigation.compose)
	//kotlinx serialization
	implementation(libs.kotlinx.serialization.json)
	//lifecycle
	implementation(libs.androidx.lifecycle.runtime.compose)
	implementation(libs.androidx.lifecycle.service)
	//kotlinx-datetime
	implementation(libs.kotlinx.datetime)
	//kotlinx-immutable
	implementation(libs.kotlinx.collections.immutable)
	//hilt
	ksp(libs.hilt.android.compiler)
	implementation(libs.hilt.android)
	//media3
	implementation(libs.androidx.media3.common)
	//splash
	implementation(libs.androidx.core.splashscreen)
	// tests
	testImplementation(libs.junit)
	testImplementation(libs.turbine)
	implementation(libs.kotlin.test)
	testImplementation(libs.kotlinx.coroutines.test)
	testImplementation(libs.assertk)
	//android tests
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.ui.test.junit4)
	//debug
	debugImplementation(libs.androidx.ui.tooling)
	debugImplementation(libs.androidx.ui.test.manifest)
}