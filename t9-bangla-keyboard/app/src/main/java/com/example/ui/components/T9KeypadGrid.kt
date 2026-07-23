package com.example.ui.components

import androidx.compose.runtime.Composable
import com.example.keyboard.KeyboardTheme
import com.example.keyboard.TypingMode

enum class CharacterCycleSet(val title: String) {
    PRIMARY("প্রাথমিক ব্যঞ্জনবর্ণ"),
    VOWELS_KARS("স্বরবর্ণ ও কার"),
    YUKTAKHOR("যুক্তাক্ষর ও বিশেষ")
}

@Composable
fun T9KeypadGrid(
    theme: KeyboardTheme,
    heightScale: Float,
    currentMode: TypingMode,
    nextKeySuggestions: Map<Char, List<Char>> = emptyMap(),
    onKeyTap: (Char) -> Unit,
    onKeyLongPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClearAll: () -> Unit,
    onModeSwitch: () -> Unit,
    onCycleSetTap: () -> Unit = {},
    activeCycleSet: CharacterCycleSet = CharacterCycleSet.PRIMARY,
    onSpace: () -> Unit
) {
    StaticT9KeypadView(
        theme = theme,
        heightScale = heightScale,
        currentMode = currentMode,
        onKeyTap = onKeyTap,
        onKeyLongPress = onKeyLongPress,
        onBackspace = onBackspace,
        onClearAll = onClearAll,
        onModeSwitch = onModeSwitch,
        onSpace = onSpace
    )
}
