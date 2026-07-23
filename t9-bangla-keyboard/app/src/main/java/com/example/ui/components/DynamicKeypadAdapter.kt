package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.dictionary.BanglaDictionary
import com.example.keyboard.KeyboardTheme
import com.example.keyboard.TypingMode

data class DynamicKeyInfo(
    val keyChar: Char,
    val digitLabel: String,
    val baseLetters: String,
    val dynamicSuggestions: List<Char> = emptyList(),
    val isSpecial: Boolean = false
)

@Composable
fun DynamicKeypadAdapter(
    theme: KeyboardTheme,
    heightScale: Float,
    currentMode: TypingMode,
    nextKeySuggestions: Map<Char, List<Char>>,
    onKeyTap: (Char) -> Unit,
    onExplicitKarTap: (String) -> Unit,
    onKeyLongPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClearAll: () -> Unit,
    onModeSwitch: () -> Unit,
    onSpace: () -> Unit
) {
    val keyHeight = (54 * heightScale).dp
    val karList = remember { listOf("া", "ি", "ী", "ু", "ূ", "ৃ", "ে", "ৈ", "ো", "ৌ", "্") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Optional Quick Kar Toolbar (Explicit Kar Disambiguation Support)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(karList) { kar ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onExplicitKarTap(kar) },
                    color = theme.primaryColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = kar,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = theme.primaryColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Base static letters with dynamic next-key highlights
        val keys = remember(nextKeySuggestions) {
            listOf(
                DynamicKeyInfo('1', "1", "অ আ ই ঈ উ ঊ ঋ এ ঐ ও ঔ", nextKeySuggestions['1'] ?: emptyList()),
                DynamicKeyInfo('2', "2", "ক খ গ ঘ ঙ", nextKeySuggestions['2'] ?: emptyList()),
                DynamicKeyInfo('3', "3", "চ ছ জ ঝ ঞ", nextKeySuggestions['3'] ?: emptyList()),
                DynamicKeyInfo('4', "4", "ট ঠ ড ঢ ণ", nextKeySuggestions['4'] ?: emptyList()),
                DynamicKeyInfo('5', "5", "ত থ দ ধ ন", nextKeySuggestions['5'] ?: emptyList()),
                DynamicKeyInfo('6', "6", "প ফ ব ভ ম", nextKeySuggestions['6'] ?: emptyList()),
                DynamicKeyInfo('7', "7", "য র ল", nextKeySuggestions['7'] ?: emptyList()),
                DynamicKeyInfo('8', "8", "শ ষ স হ", nextKeySuggestions['8'] ?: emptyList()),
                DynamicKeyInfo('9', "9", "ড় ঢ় য় ৎ ং ঃ ঁ", nextKeySuggestions['9'] ?: emptyList()),
                DynamicKeyInfo('*', "🌐", "মোড", isSpecial = true),
                DynamicKeyInfo('0', "0", "স্পেস", isSpecial = true),
                DynamicKeyInfo('#', "⌫", "মুছুন", isSpecial = true)
            )
        }

        for (rowIndex in 0..3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                for (colIndex in 0..2) {
                    val keyInfo = keys[rowIndex * 3 + colIndex]

                    DynamicKeyButton(
                        modifier = Modifier
                            .weight(1f)
                            .height(keyHeight),
                        keyInfo = keyInfo,
                        theme = theme,
                        onTap = {
                            when (keyInfo.keyChar) {
                                '*' -> onModeSwitch()
                                '0' -> onSpace()
                                '#' -> onBackspace()
                                else -> onKeyTap(keyInfo.keyChar)
                            }
                        },
                        onLongPress = {
                            when (keyInfo.keyChar) {
                                '#' -> onClearAll()
                                '*' -> onModeSwitch()
                                '0' -> onSpace()
                                else -> onKeyLongPress(keyInfo.keyChar)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicKeyButton(
    modifier: Modifier = Modifier,
    keyInfo: DynamicKeyInfo,
    theme: KeyboardTheme,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.93f else 1.0f, label = "pressScale")

    val bgCardColor = when {
        keyInfo.isSpecial -> theme.primaryColor.copy(alpha = 0.20f)
        keyInfo.dynamicSuggestions.isNotEmpty() -> theme.primaryColor.copy(alpha = 0.30f)
        isPressed -> theme.primaryColor.copy(alpha = 0.40f)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgCardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            when (keyInfo.keyChar) {
                '#' -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = theme.primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                '*' -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Mode Switch",
                            tint = theme.primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "মোড",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
                '0' -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SpaceBar,
                                contentDescription = "Space",
                                tint = theme.textColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "স্পেস",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColor,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Top row: Digit label & dynamic next predictions
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = keyInfo.digitLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor,
                                fontSize = 14.sp
                            )

                            if (keyInfo.dynamicSuggestions.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "→ ${keyInfo.dynamicSuggestions.joinToString("")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = theme.primaryColor,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Sub-label showing base letters
                        Text(
                            text = keyInfo.baseLetters,
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.textColor.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
