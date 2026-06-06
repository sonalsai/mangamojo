package com.mangamojo.app.core

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Normalized, user-presentable error type. Keeping a closed set of errors here
 * means UI code never has to reason about raw exceptions or HTTP codes.
 */
sealed class AppError(val userMessage: String) {
    data object Network : AppError("Network error — check your connection and try again.")
    data object Timeout : AppError("Request timed out — please try again.")
    data object NotFound : AppError("We couldn't find that. It may have been removed.")
    data class Server(val code: Int) : AppError("Server error ($code). Try again later.")
    data class Unknown(val detail: String?) : AppError(detail ?: "Something went wrong.")
}

/** Map any thrown exception onto a normalized [AppError]. */
fun Throwable.toAppError(): AppError = when (this) {
    is SocketTimeoutException -> AppError.Timeout
    is UnknownHostException, is IOException -> AppError.Network
    is HttpException -> when (code()) {
        404 -> AppError.NotFound
        in 500..599 -> AppError.Server(code())
        else -> AppError.Server(code())
    }
    is SerializationException -> AppError.Unknown("Unexpected response from server.")
    else -> AppError.Unknown(message)
}
