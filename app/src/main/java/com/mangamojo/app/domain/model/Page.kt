package com.mangamojo.app.domain.model

/** A single rendered page within a chapter. */
data class Page(
    val index: Int,
    val imageUrl: String,
    val headers: Map<String, String> = emptyMap(),
)
