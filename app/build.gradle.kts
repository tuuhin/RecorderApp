import com.android.build.api.dsl.ApkSigningConfig
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import shadow.bundletool.com.android.tools.r8.internal.jS

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.recorderapp.hilt)
    alias(libs.plugins.recorderapp.compose.compiler)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.eva.recorderapp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.eva.recorderapp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.compileSdk.get().toInt()
        versionCode = 15
        versionName = "1.5.0-beta1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    androidResources {
        localeFilters.addAll(setOf("bn", "hi"))
    }

    signingConfigs {
        createReleaseConfig()
    }

    buildTypes {
        release {
            applicationIdSuffix = ".release"
            // change the signing config if release is not found
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "RecorderApp (DEBUG)")
        }

        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
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

baselineProfile {
    // one merged profile for every variant (release + benchmark),
    // output to src/main/generated/baselineProfiles
    mergeIntoMain = true
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.profileinstaller)

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

    // baseline profile producer module
    baselineProfile(project(":baselineprofile"))
}


fun NamedDomainObjectContainer<out ApkSigningConfig>.createReleaseConfig() {
    val keySecretFile = rootProject.file("keystore.properties")

    var storeFileName: String? = null
    var keyAlias: String? = null
    var keyPassword: String? = null
    var storePassword: String? = null

    // first check if keystore exists in the root project
    if (keySecretFile.exists()) {
        val properties = Properties()
        keySecretFile.inputStream().use { properties.load(it) }
        storeFileName = properties.getProperty("STORE_FILE_NAME")
        keyAlias = properties.getProperty("KEY_ALIAS")
        keyPassword = properties.getProperty("KEY_PASSWORD")
        storePassword = properties.getProperty("STORE_PASSWORD")
    }

    // if not found fallback to env
    if (storeFileName.isNullOrBlank()) storeFileName = System.getenv("STORE_FILE_NAME")
    if (keyAlias.isNullOrBlank()) keyAlias = System.getenv("KEY_ALIAS")
    if (keyPassword.isNullOrBlank()) keyPassword = System.getenv("KEY_PASSWORD")
    if (storePassword.isNullOrBlank()) storePassword = System.getenv("STORE_PASSWORD")

    // finally search for the jks file
    val jksFile = project.rootProject.layout.files(storeFileName)
        .firstOrNull()

    val propertiesOk = keyAlias.isNotBlank() && keyPassword.isNotBlank() && storePassword.isNotBlank()
    val isJskFileOk = jksFile != null && jksFile.exists()

    if (!propertiesOk || !isJskFileOk) return

    // Create the release config
    create("release") {
        storeFile = jksFile
        this.keyAlias = keyAlias
        this.keyPassword = keyPassword
        this.storePassword = storePassword
    }
}
