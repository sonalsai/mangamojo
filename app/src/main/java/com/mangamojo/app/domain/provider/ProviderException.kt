package com.mangamojo.app.domain.provider

class ProviderException(
    val providerId: String,
    message: String,
    cause: Throwable? = null,
) : RuntimeException("[$providerId] $message", cause)
