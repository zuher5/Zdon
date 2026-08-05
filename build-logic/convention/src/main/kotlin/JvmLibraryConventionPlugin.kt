import com.zdon.buildlogic.applyCatalogPlugin
import com.zdon.buildlogic.configureKotlinJvm
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Convention plugin for pure-Kotlin JVM modules such as `:core:model`. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyCatalogPlugin("kotlin-jvm")
        configureKotlinJvm()
    }
}
