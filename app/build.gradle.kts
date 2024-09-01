plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.jetbrains.kotlin.android)
	alias(libs.plugins.compose.compiler)
	alias(libs.plugins.ksp)
	alias(libs.plugins.hilt)
	alias(libs.plugins.kotlinx.serialization)
	alias(libs.plugins.androidx.room)
	alias(libs.plugins.google.protobuf)
}

android {
	namespace = "com.eva.recorderapp"
	compileSdk = 35

	defaultConfig {
		applicationId = "com.eva.recorderapp"
		minSdk = 29
		targetSdk = 35
		versionCode = 2
		versionName = "1.0.1"

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
room {
	schemaDirectory("$projectDir/schemas")
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
	//icons & shapes
	implementation(libs.material.icons.extended)
	implementation(libs.androidx.graphics.shapes)
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
	ksp(libs.androidx.hilt.compiler)
	implementation(libs.hilt.android)
	//room
	implementation(libs.androidx.room.runtime)
	implementation(libs.androidx.room.ktx)
	ksp(libs.androidx.room.compiler)
	//work manager
	implementation(libs.work.runtime.ktx)
	implementation(libs.androidx.hilt.work)
	//media3
	implementation(libs.androidx.media3.common)
	implementation(libs.androidx.media3.exoplayer)
	implementation(libs.androidx.media3.ui)
	implementation(libs.androidx.media3.session)
	implementation(libs.androidx.media3.transformer)
	//splash
	implementation(libs.androidx.core.splashscreen)
	//dynamic font
	implementation(libs.androidx.ui.text.google.fonts)
	//datastore
	implementation(libs.androidx.datastore)
	implementation(libs.protobuf.javalite)
	implementation(libs.protobuf.kotlin.lite)
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

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:4.27.3"
	}
	plugins {
		create("java") {
			artifact = "com.google.protobuf:protoc-gen-javalite:3.0.0"
		}
	}

	generateProtoTasks {
		all().forEach { task ->
			task.plugins {
				create("java") {
					option("lite")
				}
				create("kotlin") {
					option("lite")
				}
			}
		}
	}
}