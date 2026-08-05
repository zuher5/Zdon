import com.android.build.api.dsl.LibraryExtension
import com.zdon.buildlogic.applyCatalogPlugin
import com.zdon.buildlogic.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Convention plugin shared by all Android library modules. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyCatalogPlugin("android-library")
        applyCatalogPlugin("kotlin-android")

        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
}
