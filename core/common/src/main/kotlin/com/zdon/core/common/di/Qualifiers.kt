package com.zdon.core.common.di

import javax.inject.Qualifier

/**
 * Qualifiers for injected [kotlinx.coroutines.CoroutineDispatcher]s so tests can
 * substitute a deterministic scheduler without touching production code.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: ZdonDispatcher)

enum class ZdonDispatcher {
    Default,
    IO,
    Main,
}

/** Qualifier for the process-lifetime [kotlinx.coroutines.CoroutineScope]. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
