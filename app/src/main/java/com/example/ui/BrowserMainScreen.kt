package com.example.ui

import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.Bookmark
import com.example.data.database.BrowserTab
import com.example.data.database.HistoryItem
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BrowserMainScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val tabs by viewModel.allTabs.collectAsStateWithLifecycle()
    val showAiSheet by viewModel.showAiAssistantSheet.collectAsStateWithLifecycle()

    var activeWebView by remember { mutableStateOf<WebView?>(null) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Intercept hardware back press:
    BackHandler(enabled = true) {
        if (showAiSheet) {
            viewModel.closeAiAssistant()
        } else if (showBookmarksDialog) {
            showBookmarksDialog = false
        } else if (showHistoryDialog) {
            showHistoryDialog = false
        } else if (showSettingsDialog) {
            showSettingsDialog = false
        } else if (currentScreen == BrowserScreen.TAB_SWITCHER) {
            viewModel.setScreen(BrowserScreen.BROWSER)
        } else if (activeWebView?.canGoBack() == true && currentUrl != "chrome_home") {
            activeWebView?.goBack()
        } else if (currentUrl != "chrome_home") {
            // Otherwise go to internal home
            viewModel.updateActiveTabDetails("chrome_home", "New Tab")
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        bottomBar = {
            if (currentScreen == BrowserScreen.BROWSER) {
                BrowserBottomBar(
                    activeWebView = activeWebView,
                    viewModel = viewModel,
                    tabsCount = tabs.size,
                    currentUrl = currentUrl
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                BrowserScreen.TAB_SWITCHER -> {
                    TabSwitcherScreen(
                        viewModel = viewModel,
                        tabs = tabs,
                        activeTab = activeTab
                    )
                }
                BrowserScreen.BROWSER -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top omnibox address bar
                        BrowserAddressBar(
                            viewModel = viewModel,
                            activeWebView = activeWebView,
                            onBookmarksClick = { showBookmarksDialog = true },
                            onHistoryClick = { showHistoryDialog = true },
                            onSettingsClick = { showSettingsDialog = true }
                        )

                        // Progress loader bar
                        val progress by viewModel.loadingProgress.collectAsStateWithLifecycle()
                        val isLoading by viewModel.isPageLoading.collectAsStateWithLifecycle()
                        if (isLoading && currentUrl != "chrome_home") {
                            LinearProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        } else {
                            Spacer(modifier = Modifier.height(3.dp))
                        }

                        // Render home screen or Web view
                        if (currentUrl == "chrome_home") {
                            ChromeHomeScreen(
                                viewModel = viewModel,
                                onBookmarkClicked = { showBookmarksDialog = true }
                            )
                        } else {
                            WebViewContainer(
                                url = currentUrl,
                                viewModel = viewModel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                onWebViewCreated = { webView ->
                                    activeWebView = webView
                                }
                            )
                        }
                    }
                }
            }

            // Gemini AI Panel - Overlay Custom Bottom Sheet
            if (showAiSheet) {
                AiAssistantSheet(
                    viewModel = viewModel,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    // Modal Overlays
    if (showBookmarksDialog) {
        BookmarksDialog(
            viewModel = viewModel,
            onDismiss = { showBookmarksDialog = false }
        )
    }

    if (showHistoryDialog) {
        HistoryDialog(
            viewModel = viewModel,
            onDismiss = { showHistoryDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

private class TabIdHolder {
    var id: Int? = null
}

// ─── WEBVIEW CONTAINER WITH TRANSITIONAL STATE RETENTION ──────────────────
@Composable
fun WebViewContainer(
    url: String,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView?) -> Unit
) {
    val context = LocalContext.current
    val tabIdHolder = remember { TabIdHolder() }
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    setSupportZoom(true)
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        viewModel.updateLoadingProgress(10)
                    }

                    override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                        super.onPageFinished(view, loadedUrl)
                        viewModel.updateLoadingProgress(100)
                        val pageTitle = view?.title ?: loadedUrl ?: "Web Page"
                        if (loadedUrl != null && loadedUrl != "about:blank" && loadedUrl != "chrome_home") {
                            viewModel.updateActiveTabDetails(loadedUrl, pageTitle)
                            viewModel.logVisit(loadedUrl, pageTitle)
                        }
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val requestUrl = request?.url?.toString() ?: return false
                        if (!requestUrl.startsWith("http://") && !requestUrl.startsWith("https://")) {
                            return true // block custom protocols like intent:// mailto: gracefully to avoid crash
                        }
                        return false // handle standard http/https normally
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        viewModel.updateLoadingProgress(newProgress)
                    }
                }
                
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            val currentTabId = activeTab?.id
            if (tabIdHolder.id != currentTabId) {
                // Save context of PREVIOUS tab first
                tabIdHolder.id?.let { oldId ->
                    val bundle = Bundle()
                    webView.saveState(bundle)
                    viewModel.webViewSavedStates[oldId] = bundle
                }

                // Restore or Load state of the CURRENT tab
                if (currentTabId != null) {
                    val savedState = viewModel.webViewSavedStates[currentTabId]
                    if (savedState != null) {
                        webView.restoreState(savedState)
                    } else {
                        if (url == "chrome_home") {
                            webView.loadUrl("about:blank")
                        } else {
                            webView.loadUrl(url)
                        }
                    }
                }
                tabIdHolder.id = currentTabId
            } else {
                // URL changed via user input
                if (url != "chrome_home" && webView.url != url) {
                    webView.loadUrl(url)
                }
            }
        },
        onRelease = { webView ->
            activeTab?.id?.let { id ->
                val bundle = Bundle()
                webView.saveState(bundle)
                viewModel.webViewSavedStates[id] = bundle
            }
            onWebViewCreated(null)
        },
        modifier = modifier
    )
}

// ─── CHROME NATIVE HOME SCREEN (NEW TAB PAGE) ──────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChromeHomeScreen(
    viewModel: BrowserViewModel,
    onBookmarkClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchEngine by viewModel.selectedSearchEngine.collectAsStateWithLifecycle()
    val history by viewModel.allHistory.collectAsStateWithLifecycle()
    val bookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()
    
    var queryInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp)
    ) {
        // Multi-colored decorative Chrome style title
        item {
            Row(
                modifier = Modifier
                    .padding(bottom = 26.dp)
                    .testTag("chrome_logo_container"),
                horizontalArrangement = Arrangement.Center
            ) {
                val letterSpacing = (-2).sp
                Text("C", color = ChromeBlue, fontSize = 46.sp, fontWeight = FontWeight.Bold, letterSpacing = letterSpacing)
                Text("h", color = ChromeRed, fontSize = 46.sp, fontWeight = FontWeight.Bold, letterSpacing = letterSpacing)
                Text("r", color = ChromeYellow, fontSize = 46.sp, fontWeight = FontWeight.Bold, letterSpacing = letterSpacing)
                Text("o", color = ChromeBlue, fontSize = 46.sp, fontWeight = FontWeight.Bold, letterSpacing = letterSpacing)
                Text("m", color = ChromeGreen, fontSize = 46.sp, fontWeight = FontWeight.Bold, letterSpacing = letterSpacing)
                Text("e", color = ChromeRed, fontSize = 46.sp, fontWeight = FontWeight.Bold, letterSpacing = letterSpacing)
            }
        }

        // Search Input Bar
        item {
            OutlinedTextField(
                value = queryInput,
                onValueChange = { queryInput = it },
                placeholder = { Text("Search or type URL", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (queryInput.isNotBlank()) {
                        IconButton(onClick = { queryInput = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        if (queryInput.isNotBlank()) {
                            val targetUrl = viewModel.getResolvedUrl(queryInput)
                            viewModel.updateActiveTabDetails(targetUrl, queryInput)
                        }
                    }
                ),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_input")
            )
        }

        // Search Engine config row
        item {
            Column(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SEARCH PROVIDER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchEngine.values().forEach { engine ->
                        val selected = engine == searchEngine
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                )
                                .clickable { viewModel.selectSearchEngine(engine) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = engine.displayName,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Quick shortcut Speed Dial tiles
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text(
                    text = "SPEED DIAL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Start)
                )
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val defaultTiles = listOf(
                        Triple("Google", "https://www.google.com", ChromeBlue),
                        Triple("Wikipedia", "https://www.wikipedia.org", Color.Gray),
                        Triple("YouTube", "https://www.youtube.com", ChromeRed),
                        Triple("GitHub", "https://github.com", Color.DarkGray),
                        Triple("Reddit", "https://www.reddit.com", ChromeYellow)
                    )

                    defaultTiles.forEach { tile ->
                        SpeedDialTile(
                            title = tile.first,
                            url = tile.second,
                            badgeColor = tile.third,
                            onTileClick = {
                                viewModel.updateActiveTabDetails(tile.second, tile.first)
                            }
                        )
                    }

                    // User custom bookmarks inside Dial
                    bookmarks.take(5).forEach { bookmark ->
                        SpeedDialTile(
                            title = bookmark.title,
                            url = bookmark.url,
                            badgeColor = ChromeGreen,
                            onTileClick = {
                                viewModel.updateActiveTabDetails(bookmark.url, bookmark.title)
                            }
                        )
                    }
                }
            }
        }

        // Recent history list snippet
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENTLY VISITED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    if (history.isNotEmpty()) {
                        Text(
                            text = "Clear All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChromeRed,
                            modifier = Modifier.clickable { viewModel.clearBrowsingHistory() }
                        )
                    }
                }

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Your browsing history and shortcuts will reside here.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.padding(top = 10.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            history.take(4).forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.updateActiveTabDetails(item.url, item.title) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = "History",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.url,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (index < 3 && index < history.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Discover Feed Section (Matches the "Elegant Dark" design mock)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Discover",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateActiveTabDetails("https://material.io/blog", "Material Design Blog") },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "The future of Android UI: How Material You is evolving in 2025",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 20.sp
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = "TechRadar • 2h ago",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedDialTile(
    title: String,
    url: String,
    badgeColor: Color,
    onTileClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clickable { onTileClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            val initial = title.firstOrNull()?.toString() ?: "W"
            Text(
                text = initial,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── TOP CHROME-STYLE ADDRESS BAR ──────────────────────────────
@Composable
fun BrowserAddressBar(
    viewModel: BrowserViewModel,
    activeWebView: WebView?,
    onBookmarksClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isCurrentPageBookmarked.collectAsStateWithLifecycle()
    val isLoading by viewModel.isPageLoading.collectAsStateWithLifecycle()

    var inputVal by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Synchronize local input value with current browser URL unless user is editing
    LaunchedEffect(currentUrl) {
        if (!isFocused) {
            inputVal = if (currentUrl == "chrome_home") "" else currentUrl
        }
    }

    Surface(
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("browser_address_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home / Logo
            IconButton(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.updateActiveTabDetails("chrome_home", "New Tab")
                }
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Home",
                    tint = if (currentUrl == "chrome_home") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            // URL input box
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(
                        1.5.dp,
                        if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection SSL safe Lock
                Icon(
                    imageVector = if (currentUrl.startsWith("https")) Icons.Default.Lock else Icons.Default.Info,
                    contentDescription = "Security Status",
                    tint = if (currentUrl.startsWith("https")) ChromeGreen else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )

                BasicTextField(
                    value = inputVal,
                    onValueChange = { inputVal = it },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Go,
                        keyboardType = KeyboardType.Uri
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            focusManager.clearFocus()
                            isFocused = false
                            val target = viewModel.getResolvedUrl(inputVal)
                            viewModel.updateActiveTabDetails(target, "Loading...")
                        }
                    ),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .focusRequester(focusRequester)
                        .testTag("omnibox_input_field")
                )

                // Inline quick buttons inside address box
                if (currentUrl != "chrome_home") {
                    // Star add bookmark
                    IconButton(
                        onClick = { viewModel.toggleCurrentPageBookmark() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Bookmark Page",
                            modifier = Modifier.size(20.dp),
                            tint = if (isBookmarked) ChromeYellow else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        )
                    }

                    // Reload/Cancel
                    IconButton(
                        onClick = {
                            if (isLoading) activeWebView?.stopLoading() else activeWebView?.reload()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = "Reload",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Gemini sparkles web assistant
                    IconButton(
                        onClick = {
                            activeWebView?.evaluateJavascript(
                                "(function() { return document.body.innerText; })();"
                            ) { text ->
                                val cleaned = text?.trim()
                                    ?.removePrefix("\"")?.removeSuffix("\"")
                                    ?.replace("\\n", "\n")
                                    ?.replace("\\t", " ") ?: ""
                                viewModel.triggerSummarizer(cleaned)
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Web Page Assistant",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Menu Dropdown and Toggle
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("New Tab") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            viewModel.openNewTab()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Bookmarks") },
                        leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onBookmarksClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Browsing History") },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onHistoryClick()
                        }
                    )
                    
                    if (currentUrl != "chrome_home") {
                        DropdownMenuItem(
                            text = { Text("AI Summarize Webpage") },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                menuExpanded = false
                                activeWebView?.evaluateJavascript(
                                    "(function() { return document.body.innerText; })();"
                                ) { text ->
                                    val cleaned = text?.trim()
                                        ?.removePrefix("\"")?.removeSuffix("\"")
                                        ?.replace("\\n", "\n") ?: ""
                                    viewModel.triggerSummarizer(cleaned)
                                }
                            }
                        )
                    }
                    
                    DropdownMenuItem(
                        text = { Text("Browser Settings") },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSettingsClick()
                        }
                    )
                }
            }
        }
    }
}

// ─── BOTTOM CHROME NAVIGATION BAR ─────────────────────────────────
@Composable
fun BrowserBottomBar(
    activeWebView: WebView?,
    viewModel: BrowserViewModel,
    tabsCount: Int,
    currentUrl: String
) {
    Surface(
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("browser_bottom_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Web back button
            IconButton(
                onClick = { activeWebView?.goBack() },
                enabled = activeWebView?.canGoBack() == true && currentUrl != "chrome_home"
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (activeWebView?.canGoBack() == true && currentUrl != "chrome_home") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            // Web forward button
            IconButton(
                onClick = { activeWebView?.goForward() },
                enabled = activeWebView?.canGoForward() == true && currentUrl != "chrome_home"
            ) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Forward",
                    tint = if (activeWebView?.canGoForward() == true && currentUrl != "chrome_home") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            // Quick add new tab button
            IconButton(
                onClick = { viewModel.openNewTab() }
            ) {
                Icon(
                    Icons.Default.AddCircleOutline,
                    contentDescription = "New Tab",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Chrome tabs count switcher button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .clickable { viewModel.setScreen(BrowserScreen.TAB_SWITCHER) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabsCount.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ─── TABS SWITCHER GRID SCREEN ────────────────────────────────────
@Composable
fun TabSwitcherScreen(
    viewModel: BrowserViewModel,
    tabs: List<BrowserTab>,
    activeTab: BrowserTab?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tabs Manager",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${tabs.size} active tabs",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = { viewModel.setScreen(BrowserScreen.BROWSER) }
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Back to browser",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Tab card grids
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(tabs) { tab ->
                    TabCard(
                        tab = tab,
                        isActive = tab.id == activeTab?.id,
                        onClick = { viewModel.switchTab(tab.id) },
                        onClose = { viewModel.closeTab(tab) }
                    )
                }
            }
        }

        // New Tab floating FAB
        FloatingActionButton(
            onClick = { viewModel.openNewTab() },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .testTag("new_tab_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Tab", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TabCard(
    tab: BrowserTab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .border(
                2.dp,
                if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .testTag("tab_card_${tab.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab header card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.05f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Web,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(6.dp))
                
                Text(
                    text = tab.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { onClose() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close tab",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Body preview representation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.WebAsset,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (tab.url == "chrome_home") "chrome://newtab" else tab.url,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }
    }
}

// ─── GEMINI SMART CO-PILOT SHEET ──────────────────────────────────
@Composable
fun AiAssistantSheet(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val summaryText by viewModel.aiSummaryText.collectAsStateWithLifecycle()
    val isWorking by viewModel.isSummarizing.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 440.dp)
            .testTag("ai_assistant_panel"),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI Web Page Assistant",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Powered by Gemini 3.5 Flash",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(onClick = { viewModel.closeAiAssistant() }) {
                    Icon(Icons.Default.Close, contentDescription = "Close panel")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Web page title info banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Web,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = activeTab?.title ?: "Web Page",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                );
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body summaries or Loading loop
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                if (isWorking) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing page content...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        item {
                            Text(
                                text = summaryText ?: "No content analyzed.",
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─── BOOKMARKS MANAGER MODAL DIALOG ──────────────────────────────
@Composable
fun BookmarksDialog(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val bookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = bookmarks.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.url.contains(searchQuery, ignoreCase = true)
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .testTag("bookmarks_modal"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = ChromeYellow, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bookmarks", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = { onDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Filter search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter bookmarks...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (bookmarks.isEmpty()) "No bookmarks added yet." else "No matching bookmarks found.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredList) { bookmark ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateActiveTabDetails(bookmark.url, bookmark.title)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bookmark.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = bookmark.url,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                IconButton(onClick = { viewModel.deleteBookmark(bookmark) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete bookmark",
                                        tint = ChromeRed.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// ─── HISTORY LOGGER MODAL DIALOG ─────────────────────────────────
@Composable
fun HistoryDialog(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val history by viewModel.allHistory.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = history.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.url.contains(searchQuery, ignoreCase = true)
    }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .testTag("history_modal"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Browsing History", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = { onDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Filter search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search logs...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (history.isEmpty()) "Browsing history is clean." else "No matches found.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredList) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateActiveTabDetails(item.url, item.title)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.url,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = dateFormatter.format(Date(item.timestamp)),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.clearBrowsingHistory()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ChromeRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Browsing Data", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── SETTINGS CONFIG MODAL DIALOG ────────────────────────────────
@Composable
fun SettingsDialog(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val activeSearchEngine by viewModel.selectedSearchEngine.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("settings_modal"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = { onDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Default Search Engine",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SearchEngine.values().forEach { engine ->
                    val selected = engine == activeSearchEngine
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.selectSearchEngine(engine) }
                            .padding(vertical = 12.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = engine.displayName,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (selected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Privacy / Warning
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Sandbox Environment Notice",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This browser environment operates using high-speed local Room caching. Your bookmarks and history persist securely on-device.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
