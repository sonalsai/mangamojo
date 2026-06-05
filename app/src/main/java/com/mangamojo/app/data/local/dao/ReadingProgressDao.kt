package com.mangamojo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mangamojo.app.data.local.entity.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingProgressDao {

    @Upsert
    suspend fun upsert(progress: ReadingProgressEntity)

    @Query("SELECT * FROM reading_progress WHERE chapterId = :chapterId")
    suspend fun getByChapter(chapterId: String): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress WHERE chapterId = :chapterId")
    fun observeByChapter(chapterId: String): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE mangaId = :mangaId")
    fun observeByManga(mangaId: String): Flow<List<ReadingProgressEntity>>

    @Query("SELECT chapterId FROM reading_progress WHERE mangaId = :mangaId AND completed = 1")
    fun observeReadChapterIds(mangaId: String): Flow<List<String>>

    @Query("DELETE FROM reading_progress WHERE chapterId = :chapterId")
    suspend fun deleteByChapter(chapterId: String)

    @Query("DELETE FROM reading_progress")
    suspend fun clear()
}
