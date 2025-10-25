import androidx.room.gradle.RoomExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class ConfigureRoomPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		target.addPlugins()
		target.addDependencies()
		target.configureRoom()
		target.configureRoomTesting()
	}

	private fun Project.addPlugins() = plugins.apply {
		val aliases = listOf("ksp", "androidx.room")
		aliases.forEach {
			catalog.findPlugin(it).ifPresent { libraryProvider ->
				val plugin = libraryProvider.get()
				apply(plugin.pluginId)
			}
		}
	}

	private fun Project.addDependencies() = dependencies.apply {
		val implementations = listOf("androidx.room.ktx", "androidx.room.runtime")
		implementations.forEach {
			catalog.findLibrary(it).ifPresent { coreKtxDependency ->
				val dependency = coreKtxDependency.get()
				add("implementation", dependency)
			}
		}
		val ksp = listOf("androidx.room.compiler")
		ksp.forEach {
			catalog.findLibrary(it).ifPresent { coreKtxDependency ->
				val dependency = coreKtxDependency.get()
				add("ksp", dependency)
			}
		}
		val androidTestImplementations = listOf("androidx.room.testing")
		androidTestImplementations.forEach {
			catalog.findLibrary(it).ifPresent { coreKtxDependency ->
				val dependency = coreKtxDependency.get()
				add("androidTestImplementation", dependency)
			}
		}
	}

	private fun Project.configureRoom() = extensions.getByType<RoomExtension>()
		.apply {
			schemaDirectory("$projectDir/schemas")
		}

	private fun Project.configureRoomTesting() = extensions.getByType<LibraryExtension>()
		.apply {
			sourceSets {
				val androidTestSourceSet = getByName("androidTest")
				androidTestSourceSet.assets.srcDir("$projectDir/schemas")
			}
		}

}