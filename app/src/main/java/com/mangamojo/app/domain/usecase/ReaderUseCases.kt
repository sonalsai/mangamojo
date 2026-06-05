package com.mangamojo.app.domain.usecase

import com.mangamojo.app.domain.model.Chapter
import com.mangamojo.app.domain.model.MangaDetails
import com.mangamojo.app.domain.model.ReadingProgress
import com.mangamojo.app.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SaveReadingProgressUseCase @Inject constructor(private val repo: LibraryRepository) {
    suspend operator fun invoke(manga: MangaDetails, chapter: Chapter, page: Int, total: Int) =
        repo.saveProgress(manga, chapter, page, total)
}

class SetChapterReadStateUseCase @Inject constructor(private val repo: LibraryRepository) {
    suspend operator fun invoke(manga: MangaDetails, chapter: Chapter, read: Boolean) =
        repo.setChapterReadState(manga, chapter, read)
}

class GetChapterProgressUseCase @Inject constructor(private val repo: LibraryRepository) {
    suspend operator fun invoke(chapterId: String): ReadingProgress? =
        repo.getChapterProgress(chapterId)
}

class ObserveReadChapterIdsUseCase @Inject constructor(private val repo: LibraryRepository) {
    operator fun invoke(mangaId: String): Flow<Set<String>> = repo.observeReadChapterIds(mangaId)
}

class ObserveMangaProgressUseCase @Inject constructor(private val repo: LibraryRepository) {
    operator fun invoke(mangaId: String): Flow<List<ReadingProgress>> = repo.observeMangaProgress(mangaId)
}
