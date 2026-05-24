package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.data.database.AppDatabase
import com.example.data.database.BrowserRepository
import com.example.ui.BrowserMainScreen
import com.example.ui.BrowserViewModel
import com.example.ui.BrowserViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup Room persistence
        val database = AppDatabase.getDatabase(this)
        val repository = BrowserRepository(
            browserTabDao = database.browserTabDao(),
            bookmarkDao = database.bookmarkDao(),
            historyItemDao = database.historyItemDao()
        )

        // Instantiate BrowserViewModel using factory mapping
        val viewModel: BrowserViewModel by viewModels {
            BrowserViewModelFactory(repository)
        }

        setContent {
            MyApplicationTheme {
                BrowserMainScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
