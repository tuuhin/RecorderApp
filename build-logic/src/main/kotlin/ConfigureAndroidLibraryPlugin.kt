import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.jvm.optionals.getOrNull

class ConfigureAndroidLibraryPlugin : Plugin<Project> {

	override fun apply(project: Project) {
		project.addPlugins()
		project.configureLibrary()
		project.configureCompileOptions()
		project.addDependencies()
	}

	private fun Project.addPlugins() = plugins.apply {
		val aliases = listOf("android.library", "jetbrains.kotlin.android")
		aliases.forEach {
			catalog.findPlugin(it).ifPresent { libraryProvider ->
				val plugin = libraryProvider.get()
				apply(plugin.pluginId)
			}
		}
	}

	private fun Project.addDependencies() = dependencies.apply {

		val implementations = listOf(
			"androidx.core.ktx",
			"androidx.lifecycle.runtime.ktx",
			"androidx.activity.compose"
		)
		implementations.forEach {
			catalog.findLibrary(it).ifPresent { dependency ->
				add("implementation", dependency.get())
			}
		}

		val testImplementations = listOf("junit", "kotlin.test", "kotlinx.coroutines.test")

		testImplementations.forEach {
			catalog.findLibrary(it).ifPresent { dependency ->
				add("testImplementation", dependency.get())
			}
		}

		val androidTestImplementations = listOf(
			"androidx.junit",
			"androidx.espresso.core",
			"kotlin.test",
			"kotlinx.coroutines.test"
		)

		androidTestImplementations.forEach {
			catalog.findLibrary(it).ifPresent { dependency ->
				add("androidTestImplementation", dependency.get())
			}
		}
	}

	private fun Project.configureLibrary() = extensions.configure<LibraryExtension> {

		compileSdk = catalog.findVersion("compileSdk").getOrNull()?.toString()?.toInt() ?: 36

		defaultConfig {
			minSdk = catalog.findVersion("minSdk").getOrNull()?.toString()?.toInt() ?: 29

			testInstrumentationRunner = Constants.TEST_RUNNER_CLASS_NAME
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

	private fun Project.configureCompileOptions() {
		tasks.withType<KotlinCompile>().configureEach {
			compilerOptions {
				jvmTarget.set(JvmTarget.JVM_17)
			}
		}
	}
}

