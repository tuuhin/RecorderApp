import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

class ConfigureComposePlugin : Plugin<Project> {

	override fun apply(target: Project) {
		target.addPlugins()
		target.addDependencies()
		target.configureCompose()
	}

	private fun Project.addPlugins() = plugins.apply {
		val aliases = listOf("compose.compiler")
		aliases.forEach {
			catalog.findPlugin(it).ifPresent { libraryProvider ->
				val plugin = libraryProvider.get()
				apply(plugin.pluginId)
			}
		}
	}

	private fun Project.addDependencies() = dependencies.apply {
		val implementations = listOf(
			"androidx.ui",
			"androidx.ui.graphics",
			"androidx.ui.tooling.preview",
			"androidx.material3"
		)
		val debugImplementation = listOf("androidx.ui.tooling", "androidx.ui.test.manifest")

		implementations.forEach {
			catalog.findLibrary(it).ifPresent { coreKtxDependency ->
				val dependency = coreKtxDependency.get()
				add("implementation", dependency)
			}
		}
		debugImplementation.forEach {
			catalog.findLibrary(it).ifPresent { coreKtxDependency ->
				val dependency = coreKtxDependency.get()
				add("debugImplementation", dependency)
			}
		}
		// platforms
		catalog.findLibrary("androidx.compose.bom").ifPresent { dependency ->
			val platform = platform(dependency.get())
			add("implementation", platform)
			add("androidTestImplementation", platform)
		}
	}

	private fun Project.configureCompose() =
		extensions.configure<ComposeCompilerGradlePluginExtension> {
//			featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)

			val compilerReportDirectory = project.layout.buildDirectory.dir("compose_compiler")
			reportsDestination.set(compilerReportDirectory)
			metricsDestination.set(compilerReportDirectory)

			stabilityConfigurationFiles.add(
				rootProject.layout.projectDirectory.file("stability_config.conf")
			)
		}
}