package com.mangamojo.app.providers.common

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

class ProviderHealthChecker(
    private val client: HttpClient,
    private val userAgent: String,
) {
    suspend fun isAvailable(baseUrl: String): Boolean =
        runCatching {
            client.get(baseUrl) {
                header(HttpHeaders.UserAgent, userAgent)
                header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            }.status.isSuccess()
        }.getOrDefault(false)
}
