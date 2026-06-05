package com.mangamojo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mangamojo.app.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY readAt DESC LIMIT :limit")
    fun observeHistory(limit: Int): Flow<List<HistoryEntity>>

    /** Recently read titles that aren't finished yet — powers "Continue reading". */
    @Query(
        "SELECT * FROM history WHERE total <= 0 OR page < total - 1 " +
            "ORDER BY readAt DESC LIMIT :limit"
    )
    fun observeContinueReading(limit: Int): Flow<List<HistoryEntity>>

    @Upsert
    suspend fun upsert(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE mangaId = :mangaId")
    suspend fun delete(mangaId: String)

    @Query("DELETE FROM history WHERE mangaId = :mangaId AND chapterId = :chapterId")
    suspend fun deleteChapter(mangaId: String, chapterId: String)

    @Query("DELETE FROM history")
    suspend fun clear()

    @Query("SELECT mangaId FROM history")
    suspend fun getHistoryMangaIds(): List<String>

    /** Keep only the most recent [limit] rows. */
    @Query(
        "DELETE FROM history WHERE mangaId NOT IN " +
            "(SELECT mangaId FROM history ORDER BY readAt DESC LIMIT :limit)"
    )
    suspend fun trimToLimit(limit: Int)
}
