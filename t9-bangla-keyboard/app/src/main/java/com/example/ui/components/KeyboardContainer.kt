package com.example.ui.components

import androidx.compose.runtime.Composable
import com.example.data.db.AppDatabase
import com.example.keyboard.KeyboardPreferences

@Composable
fun KeyboardContainer(
    prefs: KeyboardPreferences,
    database: AppDatabase,
    onCommitText: (String) -> Unit,
    onDeleteText: () -> Unit,
    onSendEnter: () -> Unit,
    onOpenSettingsInApp: (() -> Unit)? = null
) {
    KeyboardViewAdapter(
        prefs = prefs,
        database = database,
        onCommitText = onCommitText,
        onDeleteText = onDeleteText,
        onSendEnter = onSendEnter,
        onOpenSettingsInApp = onOpenSettingsInApp
    )
}

