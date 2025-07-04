import java.util.Properties

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.jetbrains.kotlin.android)

	alias(libs.plugins.recorderapp.hilt)
	alias(libs.plugins.recorderapp.compose.compiler)
}

android {
	namespace = "com.eva.recorderapp"
	compileSdk = 35

	defaultConfig {
		applicationId = "com.eva.recorderapp"
		minSdk = 29
		targetSdk = 35
		versionCode = 8
		versionName = "1.3.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		vectorDrawables {
			useSupportLibrary = true
		}

		rootProject.file("developer.properties").inputStream().use {
			val localProperties = Properties()
			localProperties.load(it)

			buildConfigField(
				"String",
				"GITHUB_PROFILE_LINK",
				"\"${localProperties.getProperty("GITHUB_PROFILE_LINK")}\""
			)

			buildConfigField(
				"String",
				"GITHUB_PROJECT_LINK",
				"\"${localProperties.getProperty("GITHUB_PROJECT_LINK")}\""
			)
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			applicationIdSuffix = ".release"
			isShrinkResources = true
			multiDexEnabled = true
			signingConfig = signingConfigs.getByName("debug")
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
	kotlinOptions {
		jvmTarget = "17"
	}
	buildFeatures {
		compose = true
		buildConfig = true
	}
	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
}

dependencies {

	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.navigation.compose)
	implementation(libs.androidx.core.splashscreen)
	implementation(libs.work.runtime.ktx)
	implementation(libs.androidx.hilt.work)

	implementation(project(":core:utils"))
	implementation(project(":core:ui"))
	implementation(project(":data:worker"))
	implementation(project(":data:interactions"))
	implementation(project(":feature:categories"))
	implementation(project(":feature:player"))
	implementation(project(":feature:recorder"))
	implementation(project(":feature:recordings"))
	implementation(project(":feature:editor"))
	implementation(project(":feature:settings"))
	implementation(project(":feature:widget"))

}
