package com.zdon.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/** Access the `libs` version catalog from convention plugin code. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** Resolve a catalog version as [Int]; fails fast when the alias is missing. */
internal fun VersionCatalog.versionInt(alias: String): Int = versionString(alias).toInt()

/** Resolve a catalog version as [String]; fails fast when the alias is missing. */
internal fun VersionCatalog.versionString(alias: String): String =
    findVersion(alias).orElseThrow {
        IllegalStateException("Version alias '$alias' is missing from libs.versions.toml")
    }.requiredVersion

/** Resolve a catalog library alias; fails fast when the alias is missing. */
internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow {
        IllegalStateException("Library alias '$alias' is missing from libs.versions.toml")
    }

/** Resolve a catalog plugin id; fails fast when the alias is missing. */
internal fun VersionCatalog.pluginId(alias: String): String =
    findPlugin(alias).orElseThrow {
        IllegalStateException("Plugin alias '$alias' is missing from libs.versions.toml")
    }.get().pluginId

/** Resolve and apply a plugin declared in the version catalog by alias. */
internal fun Project.applyCatalogPlugin(alias: String) {
    pluginManager.apply(libs.pluginId(alias))
}
