package com.zdon.core.common.result

/**
 * Minimal result wrapper used at repository boundaries so callers can react to
 * failures without exceptions crossing coroutine boundaries.
 */
sealed interface ZdonResult<out T> {

    data class Success<T>(val data: T) : ZdonResult<T>

    data class Error(val throwable: Throwable, val message: String?) : ZdonResult<Nothing>

    data object Loading : ZdonResult<Nothing>

    val dataOrNull: T?
        get() = (this as? Success)?.data
}

/** Runs [block], converting any non-cancellation throwable into [ZdonResult.Error]. */
inline fun <T> zdonRunCatching(block: () -> T): ZdonResult<T> = try {
    ZdonResult.Success(block())
} catch (cancellation: kotlinx.coroutines.CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    ZdonResult.Error(throwable, throwable.message)
}
