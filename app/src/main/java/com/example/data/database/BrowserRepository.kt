package com.example.data.database

import kotlinx.coroutines.flow.Flow

class BrowserRepository(
    private val browserTabDao: BrowserTabDao,
    private val bookmarkDao: BookmarkDao,
    private val historyItemDao: HistoryItemDao
) {
    val allTabsFlow: Flow<List<BrowserTab>> = browserTabDao.getAllTabsFlow()
    val allBookmarksFlow: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarksFlow()
    val allHistoryFlow: Flow<List<HistoryItem>> = historyItemDao.getAllHistoryFlow()

    suspend fun createTab(url: String = "chrome_home", title: String = "New Tab", activate: Boolean = true): Long {
        if (activate) {
            browserTabDao.updateAllTabsInactive()
        }
        val newTab = BrowserTab(url = url, title = title, isActive = activate)
        return browserTabDao.insertTab(newTab)
    }

    suspend fun updateTab(tab: BrowserTab) {
        browserTabDao.updateTab(tab)
    }

    suspend fun deleteTab(tab: BrowserTab) {
        browserTabDao.deleteTab(tab)
    }

    suspend fun selectTab(tabId: Int) {
        browserTabDao.selectTab(tabId)
    }

    // Bookmarks database helpers
    suspend fun addBookmark(url: String, title: String) {
        if (url.isNotBlank() && url != "chrome_home") {
            bookmarkDao.insertBookmark(Bookmark(url = url, title = title))
        }
    }

    suspend fun removeBookmarkByUrl(url: String) {
        bookmarkDao.deleteBookmarkByUrl(url)
    }

    suspend fun isBookmarked(url: String): Boolean {
        if (url == "chrome_home") return false
        return bookmarkDao.getBookmarkByUrl(url) != null
    }

    // History database helpers
    suspend fun addHistoryItem(url: String, title: String) {
        if (url.isNotBlank() && url != "chrome_home") {
            historyItemDao.insertHistoryItem(HistoryItem(url = url, title = title))
        }
    }

    suspend fun clearHistory() {
        historyItemDao.clearHistory()
    }
}
