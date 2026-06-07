package com.mangamojo.app.domain.provider

import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Provider-qualified ids let URL-backed providers pass stable route/cache keys
 * without changing the UI's existing "mangaId/chapterId" contract.
 */
object ProviderItemId {
    private const val SEPARATOR = "::"

    fun encode(providerId: String, rawId: String): String {
        val encoded = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(rawId.toByteArray(StandardCharsets.UTF_8))
        return "$providerId$SEPARATOR$encoded"
    }

    fun sourceId(id: String): String? {
        val index = id.indexOf(SEPARATOR)
        return if (index > 0) id.substring(0, index) else null
    }

    fun rawId(id: String): String {
        val index = id.indexOf(SEPARATOR)
        if (index < 0) return id
        val encoded = id.substring(index + SEPARATOR.length)
        return runCatching {
            String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)
        }.getOrDefault(id)
    }

    fun rawIdForProvider(id: String, providerId: String): String =
        if (sourceId(id) == providerId) rawId(id) else id
}
