package com.mangamojo.app.domain.usecase

import com.mangamojo.app.domain.model.Chapter
import com.mangamojo.app.domain.model.MangaCategory
import com.mangamojo.app.domain.model.MangaDetails
import com.mangamojo.app.domain.model.Page
import com.mangamojo.app.domain.model.SearchQuery
import com.mangamojo.app.domain.model.SearchResult
import com.mangamojo.app.domain.repository.MangaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchMangaUseCase @Inject constructor(private val repo: MangaRepository) {
    suspend operator fun invoke(query: SearchQuery): SearchResult = repo.search(query)
}

class GetMangaCategoriesUseCase @Inject constructor(private val repo: MangaRepository) {
    suspend operator fun invoke(): List<MangaCategory> = repo.getCategories()
}

class GetPopularMangaUseCase @Inject constructor(private val repo: MangaRepository) {
    suspend operator fun invoke(offset: Int = 0, limit: Int = 24): SearchResult =
        repo.getPopular(offset, limit)
}

class GetMangaDetailsUseCase @Inject constructor(private val repo: MangaRepository) {
    suspend operator fun invoke(mangaId: String, forceRefresh: Boolean = false): MangaDetails =
        repo.getMangaDetails(mangaId, forceRefresh)
}

class GetChaptersUseCase @Inject constructor(private val repo: MangaRepository) {
    suspend operator fun invoke(
        mangaId: String,
        languages: List<String>,
        forceRefresh: Boolean = false,
    ): List<Chapter> = repo.getChapters(mangaId, languages, forceRefresh)
}

class GetChapterPagesUseCase @Inject constructor(private val repo: MangaRepository) {
    suspend operator fun invoke(chapterId: String, dataSaver: Boolean): List<Page> =
        repo.getPages(chapterId, dataSaver)
}

class ClearCacheUseCase @Inject constructor(private val repo: MangaRepository) {
    suspend operator fun invoke() = repo.clearCache()
}

class ObserveCachedCountUseCase @Inject constructor(private val repo: MangaRepository) {
    operator fun invoke(): Flow<Int> = repo.observeCachedMangaCount()
}
