package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.data.db.AppDatabase
import com.example.keyboard.KeyboardPreferences
import com.example.ui.screens.DictionaryScreen
import com.example.ui.screens.KeyboardSetupScreen
import com.example.ui.screens.NotepadScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

enum class MainTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SETUP("সেটআপ", Icons.Default.Keyboard),
    NOTEPAD("নোটপ্যাড", Icons.Default.EditNote),
    DICTIONARY("অভিধান", Icons.Default.Book),
    SETTINGS("সেটিংস", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = KeyboardPreferences(this)
        val database = AppDatabase.getDatabase(this)

        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf(MainTab.SETUP) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            MainTab.entries.forEach { tab ->
                                val isSelected = (currentTab == tab)
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentTab = tab },
                                    icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                                    label = { Text(text = tab.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            MainTab.SETUP -> {
                                KeyboardSetupScreen(
                                    onTestKeyboardClick = { currentTab = MainTab.NOTEPAD }
                                )
                            }
                            MainTab.NOTEPAD -> {
                                NotepadScreen(
                                    database = database,
                                    prefs = prefs
                                )
                            }
                            MainTab.DICTIONARY -> {
                                DictionaryScreen(database = database)
                            }
                            MainTab.SETTINGS -> {
                                SettingsScreen(prefs = prefs)
                            }
                        }
                    }
                }
            }
        }
    }
}
