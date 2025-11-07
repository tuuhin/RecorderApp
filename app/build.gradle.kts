import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.jetbrains.kotlin.android)
	// custom plugins
	alias(libs.plugins.recorderapp.hilt)
	alias(libs.plugins.recorderapp.compose.compiler)
}

android {
	namespace = "com.eva.recorderapp"
	compileSdk = libs.versions.compileSdk.get().toInt()

	defaultConfig {
		applicationId = "com.eva.recorderapp"
		minSdk = libs.versions.minSdk.get().toInt()
		targetSdk = libs.versions.compileSdk.get().toInt()
		versionCode = 12
		versionName = "1.4.2"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		vectorDrawables {
			useSupportLibrary = true
		}
	}

	androidResources {
		localeFilters.addAll(setOf("bn", "hi"))
	}

	signingConfigs {
		// find if there is a properties file
		val keySecretFile = rootProject.file("keystore.properties")
		if (!keySecretFile.exists()) return@signingConfigs

		// load the properties
		val properties = Properties()
		keySecretFile.inputStream().use { properties.load(it) }

		val userHome = System.getProperty("user.home")
		val storeFileName = properties.getProperty("STORE_FILE_NAME")

		val keyStoreFolder = File(userHome, "keystore")
		if (!keyStoreFolder.exists()) return@signingConfigs

		val keyStoreFile = File(keyStoreFolder, storeFileName)
		if (!keyStoreFile.exists()) return@signingConfigs

		create("release") {
			storeFile = keyStoreFile
			keyAlias = properties.getProperty("KEY_ALIAS")
			keyPassword = properties.getProperty("KEY_PASSWORD")
			storePassword = properties.getProperty("STORE_PASSWORD")
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			applicationIdSuffix = ".release"
			isShrinkResources = true
			multiDexEnabled = true
			// change the signing config if release is not found
			signingConfig = signingConfigs.findByName("release")
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
		debug {
			applicationIdSuffix = ".debug"
			resValue("string", "app_name", "RecorderApp (DEBUG)")
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
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

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_17
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
	implementation(project(":feature:onboarding"))

	// android testing
	androidTestImplementation(libs.androidx.runner)
}
