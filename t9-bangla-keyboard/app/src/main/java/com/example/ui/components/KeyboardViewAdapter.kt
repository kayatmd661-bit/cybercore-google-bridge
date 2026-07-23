package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AppDatabase
import com.example.data.db.ContextPairEntity
import com.example.engine.ComposingBufferManager
import com.example.engine.DynamicAIPhoneticSolver
import com.example.keyboard.KeyboardPreferences
import com.example.keyboard.TypingMode
import com.example.voice.VoiceState
import com.example.voice.VoiceTypingManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KeyboardViewAdapter(
    prefs: KeyboardPreferences,
    database: AppDatabase,
    onCommitText: (String) -> Unit,
    onDeleteText: () -> Unit,
    onSendEnter: () -> Unit,
    onOpenSettingsInApp: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val phoneticSolver = remember { DynamicAIPhoneticSolver() }
    val bufferManager = remember { ComposingBufferManager(phoneticSolver, scope) }
    val composingState by bufferManager.state.collectAsState()

    val voiceTypingManager = remember { VoiceTypingManager(context) }
    val voiceState by voiceTypingManager.voiceState.collectAsState()

    var typingMode by remember { mutableStateOf(prefs.defaultTypingMode) }
    var customUserWords by remember { mutableStateOf<List<String>>(emptyList()) }
    var localContextPairs by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    var lastCommittedWord by remember { mutableStateOf<String?>(null) }

    var multiTapKey by remember { mutableStateOf<Char?>(null) }
    var multiTapIndex by remember { mutableStateOf(0) }
    var multiTapChar by remember { mutableStateOf<Char?>(null) }

    var showJuktoborno by remember { mutableStateOf(false) }
    var showSymbols by remember { mutableStateOf(false) }

    // Connect buffer commit callback
    LaunchedEffect(Unit) {
        bufferManager.onWordCommitted = { committedWord ->
            lastCommittedWord = committedWord
            if (committedWord.isNotBlank()) {
                scope.launch {
                    try {
                        database.contextPairDao().insertOrUpdate(
                            ContextPairEntity(
                                prevWord = lastCommittedWord ?: "",
                                nextWord = committedWord,
                                frequency = 1
                            )
                        )
                    } catch (e: Exception) {
                        // ignore DB write errors
                    }
                }
            }
        }
    }

    // Load custom user words and context pairs from SQLite database
    LaunchedEffect(composingState.rawBuffer, lastCommittedWord) {
        val dbWords = try {
            if (composingState.rawBuffer.isNotEmpty()) {
                database.userWordDao().getWordsForSequence(composingState.rawBuffer).map { it.word }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        customUserWords = dbWords

        val dbPairs = try {
            if (!lastCommittedWord.isNullOrBlank()) {
                database.contextPairDao().getNextWordPredictions(lastCommittedWord!!).map { lastCommittedWord!! to it }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        localContextPairs = dbPairs
    }

    // Multi-tap auto commit timer
    LaunchedEffect(multiTapKey, multiTapIndex) {
        if (multiTapKey != null && multiTapChar != null) {
            delay(850)
            onCommitText(multiTapChar.toString())
            multiTapKey = null
            multiTapIndex = 0
            multiTapChar = null
        }
    }

    fun handleWordCommit(word: String) {
        bufferManager.commitCurrent(chosenWord = word, autoSpace = true)
        onCommitText(word)
        if (prefs.autoSpaceAfterPunctuation) {
            onCommitText(" ")
        }
    }

    val theme = prefs.theme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.containerBgColor)
            .padding(bottom = 6.dp)
    ) {
        // Toolbar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(theme.keyBgColor.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dual Mode & Multi-Language Switcher Badge
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        typingMode = when (typingMode) {
                            TypingMode.PREDICTIVE -> TypingMode.BANGLA_QWERTY
                            TypingMode.BANGLA_QWERTY -> TypingMode.ENGLISH
                            TypingMode.ENGLISH -> TypingMode.MULTI_TAP
                            TypingMode.MULTI_TAP -> TypingMode.AVRO_PHONETIC
                            TypingMode.AVRO_PHONETIC -> TypingMode.PREDICTIVE
                            TypingMode.NUMERIC -> TypingMode.PREDICTIVE
                        }
                    },
                color = theme.primaryColor
            ) {
                Text(
                    text = typingMode.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp
                )
            }

            // Quick Control Buttons (Mic, Juktoborno, Emojis, Settings, Enter)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Native Voice Typing Mic Button
                IconButton(
                    onClick = {
                        if (voiceState is VoiceState.Listening) {
                            voiceTypingManager.stopListening()
                        } else {
                            val lang = if (typingMode == TypingMode.ENGLISH) "en-US" else "bn-BD"
                            voiceTypingManager.startListening(lang) { recognizedText ->
                                if (recognizedText.isNotBlank()) {
                                    handleWordCommit(recognizedText)
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (voiceState is VoiceState.Listening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Typing",
                        tint = if (voiceState is VoiceState.Listening) MaterialTheme.colorScheme.error else theme.primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = {
                        showJuktoborno = !showJuktoborno
                        if (showJuktoborno) showSymbols = false
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Juktoborno",
                        tint = if (showJuktoborno) theme.primaryColor else theme.textColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        showSymbols = !showSymbols
                        if (showSymbols) showJuktoborno = false
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mood,
                        contentDescription = "Emojis & Symbols",
                        tint = if (showSymbols) theme.primaryColor else theme.textColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (onOpenSettingsInApp != null) {
                    IconButton(
                        onClick = { onOpenSettingsInApp() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = theme.textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (composingState.rawBuffer.isNotEmpty()) {
                            bufferManager.commitCurrent(autoSpace = false)
                        }
                        onSendEnter()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardReturn,
                        contentDescription = "Enter",
                        tint = theme.primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Voice Listening State Bar
        AnimatedVisibility(
            visible = voiceState !is VoiceState.Idle,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.primaryColor.copy(alpha = 0.2f))
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                val stateText = when (val state = voiceState) {
                    is VoiceState.Listening -> "🎙️ কথা বলুন (Listening in ${if (typingMode == TypingMode.ENGLISH) "English" else "বাংলা"})..."
                    is VoiceState.Processing -> "⏳ প্রসেসিং হচ্ছে..."
                    is VoiceState.Success -> "✅ শোনা হয়েছে: ${state.text}"
                    is VoiceState.Error -> "⚠️ ${state.message}"
                    else -> ""
                }

                Text(
                    text = stateText,
                    color = theme.textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Candidate Predictions Bar
        if (prefs.showCandidateBar) {
            CandidatePredictionBar(
                candidates = composingState.candidates,
                currentSequence = composingState.rawBuffer,
                theme = theme,
                onSelectCandidate = { selectedWord ->
                    handleWordCommit(selectedWord)
                }
            )
        }

        // Expandable Juktoborno Helper Bar
        if (showJuktoborno) {
            JuktobornoHelperBar(
                theme = theme,
                onSelectJuktoborno = { jukto ->
                    bufferManager.onExplicitChar(jukto, customUserWords, localContextPairs)
                }
            )
        }

        // Expandable Emoji and Symbol Bar
        if (showSymbols) {
            EmojiAndSymbolBar(
                theme = theme,
                onSelectSymbol = { sym ->
                    if (composingState.rawBuffer.isNotEmpty()) {
                        bufferManager.commitCurrent(autoSpace = false)
                    }
                    onCommitText(sym)
                }
            )
        }

        // Layout Body Rendering based on active TypingMode
        when (typingMode) {
            TypingMode.PREDICTIVE -> {
                DynamicKeypadAdapter(
                    theme = theme,
                    heightScale = prefs.keyHeightScale,
                    currentMode = typingMode,
                    nextKeySuggestions = composingState.nextKeySuggestions,
                    onKeyTap = { keyChar ->
                        bufferManager.onKeyPress(keyChar, customUserWords, localContextPairs)
                    },
                    onExplicitKarTap = { karStr ->
                        bufferManager.onExplicitChar(karStr, customUserWords, localContextPairs)
                    },
                    onKeyLongPress = { keyChar ->
                        bufferManager.onExplicitChar(keyChar.toString(), customUserWords, localContextPairs)
                    },
                    onBackspace = {
                        bufferManager.onBackspace(customUserWords, localContextPairs)
                    },
                    onClearAll = {
                        bufferManager.flushBuffer()
                        onDeleteText()
                    },
                    onModeSwitch = {
                        typingMode = TypingMode.BANGLA_QWERTY
                    },
                    onSpace = {
                        if (composingState.rawBuffer.isNotEmpty()) {
                            bufferManager.commitCurrent(autoSpace = true)
                        } else {
                            onCommitText(" ")
                        }
                    }
                )
            }

            TypingMode.MULTI_TAP -> {
                StaticT9KeypadView(
                    theme = theme,
                    heightScale = prefs.keyHeightScale,
                    currentMode = typingMode,
                    onKeyTap = { keyChar ->
                        if (multiTapKey == keyChar) {
                            multiTapIndex++
                        } else {
                            if (multiTapChar != null) {
                                onCommitText(multiTapChar.toString())
                            }
                            multiTapKey = keyChar
                            multiTapIndex = 0
                        }
                        multiTapChar = com.example.data.dictionary.BanglaDictionary.keyCharMap[keyChar]?.let {
                            it[multiTapIndex % it.size]
                        } ?: keyChar
                    },
                    onKeyLongPress = { keyChar ->
                        onCommitText(keyChar.toString())
                    },
                    onBackspace = {
                        if (multiTapChar != null) {
                            multiTapKey = null
                            multiTapIndex = 0
                            multiTapChar = null
                        } else {
                            onDeleteText()
                        }
                    },
                    onClearAll = {
                        multiTapKey = null
                        multiTapChar = null
                        onDeleteText()
                    },
                    onModeSwitch = {
                        typingMode = TypingMode.BANGLA_QWERTY
                    },
                    onSpace = {
                        if (multiTapChar != null) {
                            onCommitText(multiTapChar.toString())
                            multiTapKey = null
                            multiTapChar = null
                        } else {
                            onCommitText(" ")
                        }
                    }
                )
            }

            TypingMode.BANGLA_QWERTY -> {
                QwertyKeyboardLayout(
                    theme = theme,
                    heightScale = prefs.keyHeightScale,
                    isBangla = true,
                    onKeyTap = { charStr -> onCommitText(charStr) },
                    onBackspace = { onDeleteText() },
                    onClearAll = { onDeleteText() },
                    onSpace = { onCommitText(" ") },
                    onModeSwitch = { typingMode = TypingMode.ENGLISH },
                    onSendEnter = { onSendEnter() }
                )
            }

            TypingMode.ENGLISH -> {
                QwertyKeyboardLayout(
                    theme = theme,
                    heightScale = prefs.keyHeightScale,
                    isBangla = false,
                    onKeyTap = { charStr -> onCommitText(charStr) },
                    onBackspace = { onDeleteText() },
                    onClearAll = { onDeleteText() },
                    onSpace = { onCommitText(" ") },
                    onModeSwitch = { typingMode = TypingMode.PREDICTIVE },
                    onSendEnter = { onSendEnter() }
                )
            }

            TypingMode.AVRO_PHONETIC, TypingMode.NUMERIC -> {
                QwertyKeyboardLayout(
                    theme = theme,
                    heightScale = prefs.keyHeightScale,
                    isBangla = typingMode == TypingMode.AVRO_PHONETIC,
                    onKeyTap = { charStr -> onCommitText(charStr) },
                    onBackspace = { onDeleteText() },
                    onClearAll = { onDeleteText() },
                    onSpace = { onCommitText(" ") },
                    onModeSwitch = { typingMode = TypingMode.PREDICTIVE },
                    onSendEnter = { onSendEnter() }
                )
            }
        }
    }
}
