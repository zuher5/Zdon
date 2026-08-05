package com.zdon.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * Shared Java/Kotlin configuration applied to every Android module so the
 * toolchains never drift apart between modules.
 *
 * Source/target compatibility is pinned instead of requesting a JVM toolchain so
 * the project builds with any JDK 17 or newer, which is what Android Studio ships
 * with, without triggering a toolchain download.
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = libs.versionInt("compileSdk")

        defaultConfig {
            minSdk = libs.versionInt("minSdk")
        }

        compileOptions {
            sourceCompatibility = JAVA_VERSION
            targetCompatibility = JAVA_VERSION
        }

        packaging {
            resources {
                excludes.addAll(
                    listOf(
                        "/META-INF/{AL2.0,LGPL2.1}",
                        "/META-INF/DEPENDENCIES",
                        "/META-INF/LICENSE.md",
                        "/META-INF/LICENSE-notice.md",
                        "/META-INF/NOTICE.md",
                        "/META-INF/INDEX.LIST",
                    ),
                )
            }
        }

        testOptions {
            unitTests {
                isReturnDefaultValues = true
                isIncludeAndroidResources = true
            }
        }
    }

    configureKotlinCompileTasks()
}

/** Configuration shared by pure-JVM (non Android) modules. */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JAVA_VERSION
        targetCompatibility = JAVA_VERSION
    }
    configureKotlinCompileTasks()
}

private fun Project.configureKotlinCompileTasks() {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(COMMON_COMPILER_ARGS)
        }
    }
}

private val JAVA_VERSION = JavaVersion.VERSION_17

private val COMMON_COMPILER_ARGS = listOf(
    "-Xjvm-default=all",
    // Kotlin 2.2 changed where an annotation on a constructor parameter lands.
    // Opting in to the future behaviour keeps Hilt/Dagger qualifiers applied to
    // both the parameter and the backing field, which is what DI expects.
    "-Xannotation-default-target=param-property",
)
