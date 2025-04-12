import java.util.Properties

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.jetbrains.kotlin.android)
	alias(libs.plugins.compose.compiler)
	alias(libs.plugins.ksp)
	alias(libs.plugins.hilt)
}

android {
	namespace = "com.eva.recorderapp"
	compileSdk = 35

	defaultConfig {
		applicationId = "com.eva.recorderapp"
		minSdk = 29
		targetSdk = 35
		versionCode = 7
		versionName = "1.2.2"

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


composeCompiler {

//	featureFlags = setOf(ComposeFeatureFlag.OptimizeNonSkippingGroups)
	reportsDestination = layout.buildDirectory.dir("compose_compiler")
	metricsDestination = layout.buildDirectory.dir("compose_compiler")

	stabilityConfigurationFiles.add(
		rootProject.layout.projectDirectory.file("stability_config.conf")
	)
}

dependencies {
	//android
	implementation(libs.androidx.core.ktx)

	//compose
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.ui)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.ui.tooling.preview)
	implementation(libs.androidx.material3)

	//navigation
	implementation(libs.androidx.navigation.compose)

	//hilt
	ksp(libs.hilt.android.compiler)
	ksp(libs.androidx.hilt.compiler)
	implementation(libs.hilt.android)

	//splash
	implementation(libs.androidx.core.splashscreen)
	// work manager
	implementation(libs.work.runtime.ktx)
	implementation(libs.androidx.hilt.work)

	//core
	implementation(project(":core:utils"))
	implementation(project(":core:ui"))

	//data
	implementation(project(":data:worker"))
	implementation(project(":data:interactions"))

	// feature
	implementation(project(":feature:categories"))
	implementation(project(":feature:player"))
	implementation(project(":feature:recorder"))
	implementation(project(":feature:recordings"))
	implementation(project(":feature:editor"))
	implementation(project(":feature:settings"))
	implementation(project(":feature:widget"))

}
