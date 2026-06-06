package com.mangamojo.app.domain.model

/** Browsable MangaDex tag surfaced as an app category. */
data class MangaCategory(
    val id: String,
    val name: String,
    val group: String,
)
