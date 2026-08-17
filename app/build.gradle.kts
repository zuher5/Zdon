import org.gradle.api.Project

plugins {
    alias(libs.plugins.zdon.android.application)
    alias(libs.plugins.zdon.android.application.compose)
    alias(libs.plugins.zdon.android.hilt)
}

// AGP cannot produce an App Bundle while ABI splits are enabled, because both
// want to partition the same shrunk resources (b/402800800). A bundle already
// carries every ABI and Play generates the per-device split itself, so the
// splits below are only useful for the directly distributed APKs.
val isBuildingBundle = gradle.startParameter.taskNames.any {
    it.substringAfterLast(':').startsWith("bundle")
}

/**
 * Release signing configuration, resolved from Gradle properties or environment
 * variables:
 *
 *  * `ZDON_KEYSTORE_PATH`
 *  * `ZDON_STORE_PASSWORD`
 *  * `ZDON_KEY_ALIAS`
 *  * `ZDON_KEY_PASSWORD`
 *
 * When absent, release builds fall back to the debug key so local development
 * keeps working. CI must provide the four values (via GitHub Secrets) so every
 * published APK shares one permanent signature - without that, each CI run
 * generates a fresh debug keystore and users cannot upgrade between releases.
 */
data class ReleaseSigningConfig(
    val storeFile: java.io.File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

fun Project.resolveReleaseSigningConfig(): ReleaseSigningConfig? {
    fun value(key: String): String? =
        providers.gradleProperty(key).orNull?.takeIf { it.isNotBlank() }
            ?: System.getenv(key).takeIf { it.isNotBlank() }

    val storePath = value("ZDON_KEYSTORE_PATH") ?: return null
    val storeFile = java.io.File(storePath)
    if (!storeFile.isFile) {
        logger.warn("ZDON_KEYSTORE_PATH is set but '$storePath' is not a file; using debug signing")
        return null
    }
    val storePassword = value("ZDON_STORE_PASSWORD") ?: return null
    val keyAlias = value("ZDON_KEY_ALIAS") ?: return null
    val keyPassword = value("ZDON_KEY_PASSWORD") ?: return null
    return ReleaseSigningConfig(storeFile, storePassword, keyAlias, keyPassword)
}

android {
    namespace = "com.zdon.app"

    defaultConfig {
        applicationId = "com.zdon.app"
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // yt-dlp ships a native Python runtime per ABI; only these four exist.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        }
    }

    // Per-ABI APKs keep the install size reasonable: the bundled Python and
    // FFmpeg payloads are several tens of megabytes per architecture.
    splits {
        abi {
            isEnable = !isBuildingBundle
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            isUniversalApk = true
        }
    }

    signingConfigs {
        val releaseSigning = resolveReleaseSigningConfig()
        if (releaseSigning != null) {
            create("release") {
                storeFile = releaseSigning.storeFile
                storePassword = releaseSigning.storePassword
                keyAlias = releaseSigning.keyAlias
                keyPassword = releaseSigning.keyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
            logger.lifecycle("Release signing configured from ZDON_* signing properties")
        } else {
            logger.warn(
                "No ZDON_* signing properties set; release builds will be signed with the " +
                    "debug key (not suitable for distribution).",
            )
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // A release keystore wins when configured; otherwise fall back to the
            // debug key so local builds still install. Distribution builds must
            // never rely on the fallback.
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
    }

    packaging {
        jniLibs {
            // The Python/FFmpeg payloads must exist as real files on disk because
            // they are executed as separate processes, so they cannot be compressed
            // inside the APK.
            useLegacyPackaging = true
        }
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        disable += setOf("GradleDependency", "NewerVersionAvailable")
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.designsystem)
    implementation(projects.core.downloader)
    implementation(projects.core.engine)
    implementation(projects.core.model)

    implementation(projects.feature.downloads)
    implementation(projects.feature.history)
    implementation(projects.feature.home)
    implementation(projects.feature.settings)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)

    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
