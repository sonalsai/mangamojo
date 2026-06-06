package com.mangamojo.app.data.remote

import com.mangamojo.app.data.remote.dto.AtHomeResponseDto
import com.mangamojo.app.data.remote.dto.ChapterListResponseDto
import com.mangamojo.app.data.remote.dto.MangaListResponseDto
import com.mangamojo.app.data.remote.dto.MangaResponseDto
import com.mangamojo.app.data.remote.dto.TagListResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * Raw MangaDex REST surface. Array params use the `key[]=` convention MangaDex
 * expects (Retrofit repeats them); object params (e.g. `order[chapter]=desc`)
 * are passed via [QueryMap] with bracketed keys.
 *
 * Note: no Kotlin default argument values are used here — Retrofit's dynamic
 * proxy bypasses them, so callers must supply every argument explicitly.
 */
interface MangaDexApi {

    @GET("manga")
    suspend fun searchManga(
        @Query("title") title: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("contentRating[]") contentRating: List<String>,
        @Query("includes[]") includes: List<String>,
        @Query("hasAvailableChapters") hasAvailableChapters: String?,
        @Query("includedTags[]") includedTags: List<String>?,
        @Query("includedTagsMode") includedTagsMode: String?,
        @Query("updatedAtSince") updatedAtSince: String?,
        @QueryMap order: Map<String, String>,
    ): MangaListResponseDto

    @GET("manga/{id}")
    suspend fun getManga(
        @Path("id") id: String,
        @Query("includes[]") includes: List<String>,
    ): MangaResponseDto

    @GET("manga/tag")
    suspend fun getMangaTags(): TagListResponseDto

    @GET("manga/{id}/feed")
    suspend fun getMangaFeed(
        @Path("id") id: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("translatedLanguage[]") translatedLanguage: List<String>,
        @Query("contentRating[]") contentRating: List<String>,
        @Query("includes[]") includes: List<String>,
        @QueryMap order: Map<String, String>,
    ): ChapterListResponseDto

    @GET("at-home/server/{chapterId}")
    suspend fun getAtHomeServer(
        @Path("chapterId") chapterId: String,
    ): AtHomeResponseDto
}
