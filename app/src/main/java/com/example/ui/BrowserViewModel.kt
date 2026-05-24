package com.example.ui

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.BrowserTab
import com.example.data.database.BrowserRepository
import com.example.data.database.Bookmark
import com.example.data.database.HistoryItem
import com.example.data.gemini.GeminiAssistant
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

enum class BrowserScreen {
    BROWSER,
    TAB_SWITCHER
}

enum class SearchEngine(val displayName: String, val searchUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    BING("Bing", "https://www.bing.com/search?q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    YAHOO("Yahoo", "https://search.yahoo.com/search?p=")
}

class BrowserViewModel(private val repository: BrowserRepository) : ViewModel() {

    // UI Navigation logic
    private val _currentScreen = MutableStateFlow(BrowserScreen.BROWSER)
    val currentScreen: StateFlow<BrowserScreen> = _currentScreen.asStateFlow()

    // Search Engine
    private val _selectedSearchEngine = MutableStateFlow(SearchEngine.GOOGLE)
    val selectedSearchEngine: StateFlow<SearchEngine> = _selectedSearchEngine.asStateFlow()

    // Observable states from Database
    val allTabs: StateFlow<List<BrowserTab>> = repository.allTabsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookmarks: StateFlow<List<Bookmark>> = repository.allBookmarksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHistory: StateFlow<List<HistoryItem>> = repository.allHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active tab in UI
    val activeTab: StateFlow<BrowserTab?> = allTabs
        .map { tabs -> tabs.find { it.isActive } ?: tabs.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Saved state bundles of each WebView to restore scroll/history on tab switches
    val webViewSavedStates = mutableMapOf<Int, Bundle>()

    // Current Web URL and Title (dynamically updated by active WebView)
    private val _currentUrl = MutableStateFlow("chrome_home")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _currentTitle = MutableStateFlow("New Tab")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    // Is current URL bookmarked?
    val isCurrentPageBookmarked: StateFlow<Boolean> = combine(currentUrl, allBookmarks) { url, bookmarks ->
        if (url == "chrome_home") false
        else bookmarks.any { it.url == url }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Loading Progress (0-100)
    private val _loadingProgress = MutableStateFlow(0)
    val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()

    private val _isPageLoading = MutableStateFlow(false)
    val isPageLoading: StateFlow<Boolean> = _isPageLoading.asStateFlow()

    // Gemini AI Web Assistant States
    private val _aiSummaryText = MutableStateFlow<String?>(null)
    val aiSummaryText: StateFlow<String?> = _aiSummaryText.asStateFlow()

    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing: StateFlow<Boolean> = _isSummarizing.asStateFlow()

    private val _showAiAssistantSheet = MutableStateFlow(false)
    val showAiAssistantSheet: StateFlow<Boolean> = _showAiAssistantSheet.asStateFlow()

    // Initialize default tabs if empty and sync current URL/Title states
    init {
        viewModelScope.launch {
            repository.allTabsFlow.firstOrNull()?.let { tabs ->
                if (tabs.isEmpty()) {
                    repository.createTab(url = "chrome_home", title = "New Tab", activate = true)
                }
            }
        }

        // Sync local currentUrl and currentTitle states with the DB activeTab flow
        viewModelScope.launch {
            activeTab.collect { tab ->
                if (tab != null) {
                    _currentUrl.value = tab.url
                    _currentTitle.value = tab.title
                } else {
                    _currentUrl.value = "chrome_home"
                    _currentTitle.value = "New Tab"
                }
            }
        }
    }

    fun setScreen(screen: BrowserScreen) {
        _currentScreen.value = screen
    }

    fun selectSearchEngine(engine: SearchEngine) {
        _selectedSearchEngine.value = engine
    }

    fun updateLoadingProgress(progress: Int) {
        _loadingProgress.value = progress
        _isPageLoading.value = progress in 1..99
    }

    // Tab Management Actions
    fun openNewTab(url: String = "chrome_home", title: String = "New Tab") {
        viewModelScope.launch {
            repository.createTab(url, title, activate = true)
            _currentScreen.value = BrowserScreen.BROWSER
        }
    }

    fun closeTab(tab: BrowserTab) {
        viewModelScope.launch {
            webViewSavedStates.remove(tab.id)
            repository.deleteTab(tab)
            
            // If we closed the active tab, locate and activate another one
            if (tab.isActive) {
                val currentTabs = allTabs.value.filter { it.id != tab.id }
                if (currentTabs.isNotEmpty()) {
                    repository.selectTab(currentTabs.last().id)
                } else {
                    repository.createTab("chrome_home", "New Tab", activate = true)
                }
            }
        }
    }

    fun switchTab(tabId: Int) {
        viewModelScope.launch {
            // First, if we have an active tab loaded, request that Compose/UI save its current state
            // It will be saved into our webViewSavedStates map.
            repository.selectTab(tabId)
            _currentScreen.value = BrowserScreen.BROWSER
            // Reset AI state
            closeAiAssistant()
        }
    }

    fun updateActiveTabDetails(url: String, title: String) {
        _currentUrl.value = url
        _currentTitle.value = title
        
        viewModelScope.launch {
            activeTab.value?.let { tab ->
                if (tab.url != url || tab.title != title) {
                    repository.updateTab(tab.copy(url = url, title = title))
                }
            }
        }
    }

    // Bookmark Actions
    fun toggleCurrentPageBookmark() {
        val url = _currentUrl.value
        val title = _currentTitle.value
        if (url == "chrome_home") return

        viewModelScope.launch {
            if (isCurrentPageBookmarked.value) {
                repository.removeBookmarkByUrl(url)
            } else {
                repository.addBookmark(url, title)
            }
        }
    }

    fun addManualBookmark(url: String, title: String) {
        viewModelScope.launch {
            repository.addBookmark(url, title)
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.removeBookmarkByUrl(bookmark.url)
        }
    }

    // History Actions
    fun logVisit(url: String, title: String) {
        if (url.isBlank() || url == "chrome_home") return
        viewModelScope.launch {
            repository.addHistoryItem(url, title)
        }
    }

    fun clearBrowsingHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Utility address/search parser
    fun getResolvedUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return "chrome_home"
        
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        
        // Check for common local page addresses
        if (trimmed == "chrome://newtab" || trimmed == "chrome_home") {
            return "chrome_home"
        }
        
        // Simple domain heuristic (contains a dot and no spaces)
        val hasSpace = trimmed.contains(" ")
        val hasDot = trimmed.contains(".") && !trimmed.endsWith(".")
        if (!hasSpace && hasDot) {
            return "https://$trimmed"
        }
        
        // Otherwise, run search query
        return try {
            _selectedSearchEngine.value.searchUrl + URLEncoder.encode(trimmed, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            _selectedSearchEngine.value.searchUrl + trimmed
        }
    }

    // Gemini Helper Actions
    fun triggerSummarizer(textContent: String) {
        val title = _currentTitle.value
        _showAiAssistantSheet.value = true
        _isSummarizing.value = true
        _aiSummaryText.value = null
        
        viewModelScope.launch {
            val result = GeminiAssistant.summarizeWebPage(title, textContent)
            _aiSummaryText.value = result
            _isSummarizing.value = false
        }
    }

    fun closeAiAssistant() {
        _showAiAssistantSheet.value = false
        _aiSummaryText.value = null
        _isSummarizing.value = false
    }
}

class BrowserViewModelFactory(private val repository: BrowserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BrowserViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
