import com.android.build.api.dsl.ApplicationExtension
import com.zdon.buildlogic.applyCatalogPlugin
import com.zdon.buildlogic.configureKotlinAndroid
import com.zdon.buildlogic.libs
import com.zdon.buildlogic.versionInt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin for the single application module. Keeps the app module build
 * script free of boilerplate and guarantees identical Java/Kotlin settings with
 * every library module.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyCatalogPlugin("android-application")
        applyCatalogPlugin("kotlin-android")

        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            defaultConfig.targetSdk = libs.versionInt("targetSdk")
            buildFeatures.buildConfig = true
        }
    }
}
