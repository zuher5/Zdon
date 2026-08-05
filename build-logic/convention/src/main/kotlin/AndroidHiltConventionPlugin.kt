import com.zdon.buildlogic.applyCatalogPlugin
import com.zdon.buildlogic.library
import com.zdon.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Wires Hilt + KSP into any Android module that needs dependency injection. */
class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyCatalogPlugin("ksp")
        applyCatalogPlugin("hilt")

        dependencies {
            add("implementation", libs.library("hilt-android"))
            add("ksp", libs.library("hilt-compiler"))
            add("testImplementation", libs.library("hilt-android-testing"))
            add("kspTest", libs.library("hilt-compiler"))
        }
    }
}
