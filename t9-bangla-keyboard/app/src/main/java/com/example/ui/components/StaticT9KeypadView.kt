package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyboard.KeyboardTheme
import com.example.keyboard.TypingMode

data class KeypadButtonInfo(
    val keyChar: Char,
    val mainLabel: String,
    val subLabel: String,
    val isSpecial: Boolean = false
)

@Composable
fun StaticT9KeypadView(
    theme: KeyboardTheme,
    heightScale: Float,
    currentMode: TypingMode,
    onKeyTap: (Char) -> Unit,
    onKeyLongPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClearAll: () -> Unit,
    onModeSwitch: () -> Unit,
    onSpace: () -> Unit
) {
    val keyHeight = (56 * heightScale).dp

    // STATIC 3x4 KEYBOARD LAYOUT - BASE ALPHABETS ONLY (STRICTLY NO KARS ON BUTTONS)
    val keys = remember {
        listOf(
            KeypadButtonInfo('1', "1", "অ আ ই ঈ উ ঊ ঋ এ ঐ ও ঔ"),
            KeypadButtonInfo('2', "2", "ক খ গ ঘ ঙ"),
            KeypadButtonInfo('3', "3", "চ ছ জ ঝ ঞ"),
            KeypadButtonInfo('4', "4", "ট ঠ ড ঢ ণ"),
            KeypadButtonInfo('5', "5", "ত থ দ ধ ন"),
            KeypadButtonInfo('6', "6", "প ফ ব ভ ম"),
            KeypadButtonInfo('7', "7", "য র ল"),
            KeypadButtonInfo('8', "8", "শ ষ স হ"),
            KeypadButtonInfo('9', "9", "ড় ঢ় য় ৎ ং ঃ ঁ"),
            KeypadButtonInfo('*', "🌐", "মোড", isSpecial = true),
            KeypadButtonInfo('0', "0", "স্পেস", isSpecial = true),
            KeypadButtonInfo('#', "⌫", "মুছুন", isSpecial = true)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (rowIndex in 0..3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (colIndex in 0..2) {
                    val keyInfo = keys[rowIndex * 3 + colIndex]

                    StaticT9KeyButton(
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
fun StaticT9KeyButton(
    modifier: Modifier = Modifier,
    keyInfo: KeypadButtonInfo,
    theme: KeyboardTheme,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.93f else 1.0f, label = "pressScale")

    val bgCardColor = when {
        keyInfo.isSpecial -> theme.primaryColor.copy(alpha = 0.20f)
        isPressed -> theme.primaryColor.copy(alpha = 0.35f)
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
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            when (keyInfo.keyChar) {
                '#' -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = theme.primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.textColor.copy(alpha = 0.55f),
                            fontSize = 9.sp
                        )
                    }
                }
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = keyInfo.mainLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = keyInfo.subLabel,
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
