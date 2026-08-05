package com.zdon.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Enables Jetpack Compose for a module and wires the Compose BOM plus the
 * baseline set of Compose artifacts every UI module in Zdon relies on.
 */
internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    applyCatalogPlugin("compose-compiler")

    commonExtension.apply {
        buildFeatures {
            compose = true
        }
    }

    dependencies {
        val bom = libs.library("androidx-compose-bom")
        add("implementation", platform(bom))
        add("androidTestImplementation", platform(bom))

        add("implementation", libs.library("androidx-compose-foundation"))
        add("implementation", libs.library("androidx-compose-material-icons-extended"))
        add("implementation", libs.library("androidx-compose-material3"))
        add("implementation", libs.library("androidx-compose-material3-window-size"))
        add("implementation", libs.library("androidx-compose-runtime"))
        add("implementation", libs.library("androidx-compose-ui"))
        add("implementation", libs.library("androidx-compose-ui-graphics"))
        add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
        add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
        add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))

        add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
        add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))

        add("androidTestImplementation", libs.library("androidx-compose-ui-test-junit4"))
    }
}
