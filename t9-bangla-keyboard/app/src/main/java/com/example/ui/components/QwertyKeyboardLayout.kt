package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyboard.KeyboardTheme
import com.example.keyboard.TypingMode

@Composable
fun QwertyKeyboardLayout(
    theme: KeyboardTheme,
    heightScale: Float,
    isBangla: Boolean,
    onKeyTap: (String) -> Unit,
    onBackspace: () -> Unit,
    onClearAll: () -> Unit,
    onSpace: () -> Unit,
    onModeSwitch: () -> Unit,
    onSendEnter: () -> Unit
) {
    var isShifted by remember { mutableStateOf(false) }

    val keyRow1 = if (isBangla) {
        if (!isShifted) listOf("ক", "খ", "গ", "ঘ", "ঙ", "চ", "ছ", "জ", "ঝ", "ঞ")
        else listOf("অ", "আ", "ই", "ঈ", "উ", "ঊ", "ঋ", "এ", "ঐ", "ও")
    } else {
        if (!isShifted) listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        else listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
    }

    val keyRow2 = if (isBangla) {
        if (!isShifted) listOf("ট", "ঠ", "ড", "ঢ", "ণ", "ত", "থ", "দ", "ধ", "ন")
        else listOf("া", "ি", "ী", "ু", "ূ", "ৃ", "ে", "ৈ", "ো", "ৌ")
    } else {
        if (!isShifted) listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        else listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
    }

    val keyRow3 = if (isBangla) {
        if (!isShifted) listOf("প", "ফ", "ব", "ভ", "ম", "য", "র", "ল", "শ")
        else listOf("ষ", "স", "হ", "ড়", "ঢ়", "য়", "ৎ", "ং", "ঃ")
    } else {
        if (!isShifted) listOf("z", "x", "c", "v", "b", "n", "m")
        else listOf("Z", "X", "C", "V", "B", "N", "M")
    }

    val keyHeight = (44 * heightScale).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (char in keyRow1) {
                QwertyKey(
                    modifier = Modifier
                        .weight(1f)
                        .height(keyHeight),
                    label = char,
                    theme = theme,
                    onTap = { onKeyTap(char) }
                )
            }
        }

        // Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (char in keyRow2) {
                QwertyKey(
                    modifier = Modifier
                        .weight(1f)
                        .height(keyHeight),
                    label = char,
                    theme = theme,
                    onTap = { onKeyTap(char) }
                )
            }
        }

        // Row 3 (Shift + Keys + Backspace)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shift / Kar Key
            QwertyKey(
                modifier = Modifier
                    .weight(1.3f)
                    .height(keyHeight),
                label = if (isShifted) "⇧" else "⇧",
                theme = theme,
                isSpecial = true,
                onTap = { isShifted = !isShifted }
            )

            for (char in keyRow3) {
                QwertyKey(
                    modifier = Modifier
                        .weight(1f)
                        .height(keyHeight),
                    label = char,
                    theme = theme,
                    onTap = { onKeyTap(char) }
                )
            }

            // Backspace Key
            QwertyKey(
                modifier = Modifier
                    .weight(1.3f)
                    .height(keyHeight),
                label = "⌫",
                theme = theme,
                isSpecial = true,
                onTap = { onBackspace() },
                onLongPress = { onClearAll() }
            )
        }

        // Row 4 (Mode, Space, Enter)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language/Mode Switcher
            QwertyKey(
                modifier = Modifier
                    .weight(1.5f)
                    .height(keyHeight),
                label = "🌐",
                theme = theme,
                isSpecial = true,
                onTap = { onModeSwitch() }
            )

            // Spacebar
            QwertyKey(
                modifier = Modifier
                    .weight(5f)
                    .height(keyHeight),
                label = "স্পেস / Space",
                theme = theme,
                onTap = { onSpace() }
            )

            // Enter Key
            QwertyKey(
                modifier = Modifier
                    .weight(1.5f)
                    .height(keyHeight),
                label = "↵",
                theme = theme,
                isSpecial = true,
                onTap = { onSendEnter() }
            )
        }
    }
}

@Composable
fun QwertyKey(
    modifier: Modifier = Modifier,
    label: String,
    theme: KeyboardTheme,
    isSpecial: Boolean = false,
    onTap: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1.0f, label = "pressScale")

    val bgCardColor = when {
        isSpecial -> theme.primaryColor.copy(alpha = 0.25f)
        isPressed -> theme.primaryColor.copy(alpha = 0.4f)
        else -> theme.keyBgColor
    }

    Card(
        modifier = modifier
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgCardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSpecial) theme.primaryColor else theme.textColor,
                fontSize = if (label.length > 2) 11.sp else 15.sp
            )
        }
    }
}
