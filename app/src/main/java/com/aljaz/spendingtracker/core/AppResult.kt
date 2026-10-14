package com.aljaz.spendingtracker.core

/**
 * Outcome of an operation that can fail in an expected way
 * (for example, validation errors).
 *
 * Callers must handle both cases, so failures are never ignored.
 */
sealed interface AppResult<out T> {

    /** The operation worked; [data] is its result. */
    data class Success<T>(val data: T) : AppResult<T>

    /** The operation failed; [message] explains why, in plain language. */
    data class Error(val message: String) : AppResult<Nothing>
}