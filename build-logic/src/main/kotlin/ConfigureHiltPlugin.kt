import org.gradle.api.Plugin
import org.gradle.api.Project

class ConfigureHiltPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		target.addPlugins()
		target.addHiltDependencies()
	}

	private fun Project.addPlugins() = plugins.apply {
		val aliases = listOf("ksp", "hilt")
		aliases.forEach {
			catalog.findPlugin(it).ifPresent { libraryProvider ->
				val plugin = libraryProvider.get()
				apply(plugin.pluginId)
			}
		}
	}

	private fun Project.addHiltDependencies() = dependencies.apply {
		val implementations = listOf("hilt.android")
		implementations.forEach {
			catalog.findLibrary(it).ifPresent { coreKtxDependency ->
				val dependency = coreKtxDependency.get()
				add("implementation", dependency)
			}
		}
		val ksp = listOf("hilt.android.compiler", "androidx.hilt.compiler")
		ksp.forEach {
			catalog.findLibrary(it).ifPresent { coreKtxDependency ->
				val dependency = coreKtxDependency.get()
				add("ksp", dependency)
			}
		}
		val androidTest = listOf("hilt.test")
		androidTest.forEach {
			catalog.findLibrary(it).ifPresent { androidTestDependency ->
				val dependency = androidTestDependency.get()
				add("androidTestImplementation", dependency)
			}
		}
		val kspAndroidText = listOf("hilt.android.compiler")
		kspAndroidText.forEach {
			catalog.findLibrary(it).ifPresent { testDependency ->
				val dependency = testDependency.get()
				add("kspAndroidTest", dependency)
			}
		}
	}

}