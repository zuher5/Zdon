import com.google.devtools.ksp.gradle.KspExtension
import com.zdon.buildlogic.applyCatalogPlugin
import com.zdon.buildlogic.library
import com.zdon.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/** Wires Room + KSP and points the schema exporter at a stable location. */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyCatalogPlugin("ksp")

        val schemaDirectory = layout.projectDirectory.dir("schemas")

        extensions.configure<KspExtension> {
            arg("room.schemaLocation", schemaDirectory.asFile.absolutePath)
            arg("room.generateKotlin", "true")
        }

        dependencies {
            add("implementation", libs.library("androidx-room-runtime"))
            add("implementation", libs.library("androidx-room-ktx"))
            add("ksp", libs.library("androidx-room-compiler"))
        }

        // Declaring the schema directory as an input keeps migration tests
        // correctly invalidated when a schema changes.
        tasks.withType<Test>().configureEach {
            inputs.dir(schemaDirectory)
                .withPropertyName("roomSchemas")
                .withPathSensitivity(PathSensitivity.RELATIVE)
        }
    }
}
