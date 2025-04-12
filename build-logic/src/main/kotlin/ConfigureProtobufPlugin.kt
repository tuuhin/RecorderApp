import com.google.protobuf.gradle.ProtobufExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class ConfigureProtobufPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		target.addPlugins()
		target.addDependencies()
		target.configureProtobuf()
	}

	private fun Project.addPlugins() = plugins.apply {
		val aliases = listOf("google.protobuf")
		aliases.forEach {
			catalog.findPlugin(it).ifPresent { libraryProvider ->
				val plugin = libraryProvider.get()
				apply(plugin.pluginId)
			}
		}
	}

	private fun Project.addDependencies() = dependencies.apply {
		val implementations = listOf("protobuf.javalite", "protobuf.kotlin.lite")
		implementations.forEach {
			catalog.findLibrary(it).ifPresent { coreKtxDependency ->
				val dependency = coreKtxDependency.get()
				add("implementation", dependency)
			}
		}
	}

	private fun Project.configureProtobuf() = extensions.configure<ProtobufExtension> {

		val protocArtifact = catalog.findLibrary("protobuf.protoc").get()
		val protocJavaGen = catalog.findLibrary("protobuf.gen.javalite").get()

		protoc {
			artifact = protocArtifact.get().toString()
		}
		plugins {
			create("java") {
				artifact = protocJavaGen.get().toString()
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
}