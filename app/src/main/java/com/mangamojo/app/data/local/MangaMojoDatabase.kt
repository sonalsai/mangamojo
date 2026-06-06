package com.mangamojo.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mangamojo.app.data.local.dao.BookmarkDao
import com.mangamojo.app.data.local.dao.FavoriteDao
import com.mangamojo.app.data.local.dao.HistoryDao
import com.mangamojo.app.data.local.dao.MangaCacheDao
import com.mangamojo.app.data.local.dao.ReadingProgressDao
import com.mangamojo.app.data.local.entity.BookmarkEntity
import com.mangamojo.app.data.local.entity.CachedChapterEntity
import com.mangamojo.app.data.local.entity.CachedMangaEntity
import com.mangamojo.app.data.local.entity.FavoriteEntity
import com.mangamojo.app.data.local.entity.HistoryEntity
import com.mangamojo.app.data.local.entity.ReadingProgressEntity

@Database(
    entities = [
        CachedMangaEntity::class,
        CachedChapterEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        ReadingProgressEntity::class,
        BookmarkEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class MangaMojoDatabase : RoomDatabase() {
    abstract fun mangaCacheDao(): MangaCacheDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        const val NAME = "mangamojo.db"
    }
}
