package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserTabDao {
    @Query("SELECT * FROM browser_tabs ORDER BY createdAt ASC")
    fun getAllTabsFlow(): Flow<List<BrowserTab>>

    @Query("SELECT * FROM browser_tabs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTab(): BrowserTab?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: BrowserTab): Long

    @Update
    suspend fun updateTab(tab: BrowserTab)

    @Delete
    suspend fun deleteTab(tab: BrowserTab)

    @Query("DELETE FROM browser_tabs")
    suspend fun clearAllTabs()

    @Query("UPDATE browser_tabs SET isActive = 0")
    suspend fun updateAllTabsInactive()

    @Query("UPDATE browser_tabs SET isActive = (CASE WHEN id = :tabId THEN 1 ELSE 0 END)")
    suspend fun selectTab(tabId: Int)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarksFlow(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun getBookmarkByUrl(url: String): Bookmark?
}

@Dao
interface HistoryItemDao {
    @Query("SELECT * FROM history_items ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: HistoryItem)

    @Delete
    suspend fun deleteHistoryItem(item: HistoryItem)

    @Query("DELETE FROM history_items")
    suspend fun clearHistory()
}
