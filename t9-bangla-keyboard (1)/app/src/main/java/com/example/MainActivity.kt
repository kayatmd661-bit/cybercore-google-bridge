package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.t9keyboard.KeyboardMode
import com.example.t9keyboard.data.T9Word
import com.example.t9keyboard.logic.T9Engine
import com.example.t9keyboard.ui.T9ViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: T9ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                T9KeyboardApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun T9KeyboardApp(viewModel: T9ViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var isImeEnabled by remember { mutableStateOf(false) }
    var showAddWordDialog by remember { mutableStateOf(false) }

    // Check if IME is enabled in settings
    LaunchedEffect(Unit) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledImes = imm.enabledInputMethodList
        isImeEnabled = enabledImes.any { it.packageName == context.packageName }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("T9", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("T9 Bangla Keyboard", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("AI-Powered 3x4 Keypad", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddWordDialog = true },
                    modifier = Modifier.testTag("add_word_fab"),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Word")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Keypad Sandbox") },
                    icon = { Icon(Icons.Default.Keyboard, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Dictionary") },
                    icon = { Icon(Icons.Default.Translate, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Gemini AI") },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> KeypadSandboxTab(context, isImeEnabled, viewModel)
                1 -> DictionaryTab(viewModel)
                2 -> GeminiAiTab(viewModel)
            }
        }
    }

    if (showAddWordDialog) {
        AddWordDialog(
            viewModel = viewModel,
            onDismiss = { showAddWordDialog = false }
        )
    }
}

@Composable
fun KeypadSandboxTab(context: Context, isImeEnabled: Boolean, viewModel: T9ViewModel) {
    var typedText by remember { mutableStateOf("") }
    var currentSeq by remember { mutableStateOf("") }
    var isBanglaMode by remember { mutableStateOf(true) }
    var currentMode by remember { mutableStateOf(KeyboardMode.T9) }
    val allWords by viewModel.allWords.collectAsState()

    var candidates by remember { mutableStateOf<List<String>>(emptyList()) }

    fun getDigitChar(digit: String, isBangla: Boolean): String {
        if (!isBangla) return digit
        return when (digit) {
            "1" -> "১"; "2" -> "২"; "3" -> "৩"; "4" -> "৪"
            "5" -> "৫"; "6" -> "৬"; "7" -> "৭"; "8" -> "৮"
            "9" -> "৯"; "0" -> "০"
            else -> digit
        }
    }

    LaunchedEffect(currentSeq, isBanglaMode, currentMode, typedText, allWords) {
        if (currentMode != KeyboardMode.T9) {
            candidates = emptyList()
        } else if (currentSeq.isEmpty()) {
            candidates = T9Engine.getNextWordPredictions(typedText, isBanglaMode)
        } else {
            candidates = viewModel.getSuggestions(currentSeq, isBanglaMode, typedText)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // IME Setup Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isImeEnabled) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isImeEnabled) Icons.Default.CheckCircle else Icons.Default.Settings,
                            contentDescription = null,
                            tint = if (isImeEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isImeEnabled) "T9 Keyboard Ready" else "Enable System Keyboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isImeEnabled) "System keyboard is configured! Tap below to switch or use the on-screen keypad playground."
                        else "Enable T9 AI Bangla Keyboard in Android Input Settings to use across all apps.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                            },
                            modifier = Modifier.testTag("enable_keyboard_button")
                        ) {
                            Text("1. Enable Keyboard")
                        }
                        OutlinedButton(
                            onClick = {
                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.showInputMethodPicker()
                            },
                            modifier = Modifier.testTag("select_keyboard_button")
                        ) {
                            Text("2. Select active IME")
                        }
                    }
                }
            }
        }

        // Live Output Display
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Interactive Keypad Playground",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = typedText,
                        onValueChange = { typedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sandbox_text_input"),
                        label = { Text("Typed Result") },
                        placeholder = { Text("Use the 3x4 T9 keypad below...") },
                        trailingIcon = {
                            if (typedText.isNotEmpty()) {
                                IconButton(onClick = { typedText = "" }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                }
            }
        }

        // Candidates Bar
        item {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Suggestions: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(4.dp))
                    if (candidates.isEmpty()) {
                        Text("Type digits on 3x4 keypad", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(candidates) { word ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        val prevWord = typedText.trim().split(Regex("\\s+")).lastOrNull() ?: ""
                                        if (prevWord.isNotEmpty()) {
                                            T9Engine.recordWordPair(prevWord, word)
                                        }
                                        typedText += "$word "
                                        currentSeq = ""
                                    }
                                ) {
                                    Text(
                                        text = word,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Built-in T9 3x4 Keypad Composable
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "3x4 Keypad (${if (isBanglaMode) "BN" else "EN"})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Mode: ${currentMode.name}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Mode Switcher Buttons (T9 / 123 / ABC)
                            OutlinedButton(
                                onClick = {
                                    currentMode = when (currentMode) {
                                        KeyboardMode.T9 -> KeyboardMode.NUM
                                        KeyboardMode.NUM -> KeyboardMode.TEXT
                                        KeyboardMode.TEXT -> KeyboardMode.T9
                                    }
                                    currentSeq = ""
                                },
                                modifier = Modifier.testTag("toggle_mode_button")
                            ) {
                                Text(
                                    when (currentMode) {
                                        KeyboardMode.T9 -> "T9"
                                        KeyboardMode.NUM -> "123"
                                        KeyboardMode.TEXT -> "ABC"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    isBanglaMode = !isBanglaMode
                                    currentSeq = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier.testTag("toggle_lang_button")
                            ) {
                                Text(if (isBanglaMode) "BN" else "EN", fontSize = 12.sp)
                            }
                        }
                    }

                    val keys = listOf(
                        KeyInfo("1", ".,?!", "অআই"),
                        KeyInfo("2", "ABC", "কখগ"),
                        KeyInfo("3", "DEF", "চছজ"),
                        KeyInfo("4", "GHI", "টঠড"),
                        KeyInfo("5", "JKL", "তথদ"),
                        KeyInfo("6", "MNO", "পফব"),
                        KeyInfo("7", "PQRS", "যরল"),
                        KeyInfo("8", "TUV", "ষসহ"),
                        KeyInfo("9", "WXYZ", "ািে")
                    )

                    val dynamicLabels = remember(currentSeq, isBanglaMode, typedText) {
                        T9Engine.getDynamicKeyLabels(currentSeq, isBanglaMode, typedText)
                    }

                    for (r in 0 until 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (c in 0 until 3) {
                                val key = keys[r * 3 + c]
                                val dynamicLabel = dynamicLabels[key.digit.first()] ?: if (isBanglaMode) key.bnLabel else key.enLabel
                                KeypadButton(
                                    digit = key.digit,
                                    label = dynamicLabel,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        when (currentMode) {
                                            KeyboardMode.NUM -> {
                                                typedText += getDigitChar(key.digit, isBanglaMode)
                                            }
                                            KeyboardMode.T9 -> {
                                                currentSeq += key.digit
                                            }
                                            KeyboardMode.TEXT -> {
                                                val charMap = T9Engine.getCharMappings(key.digit.first(), isBanglaMode)
                                                typedText += charMap.firstOrNull() ?: key.digit
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        val numChar = getDigitChar(key.digit, isBanglaMode)
                                        typedText += numChar
                                        currentSeq = ""
                                        Toast.makeText(context, "Direct digit: $numChar", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    // Bottom Row: [Space] [Backspace]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val prevWord = typedText.trim().split(Regex("\\s+")).lastOrNull() ?: ""
                                if (candidates.isNotEmpty()) {
                                    val word = candidates.first()
                                    if (prevWord.isNotEmpty()) {
                                        T9Engine.recordWordPair(prevWord, word)
                                    }
                                    typedText += "$word "
                                } else {
                                    typedText += " "
                                }
                                currentSeq = ""
                            },
                            modifier = Modifier
                                .weight(2f)
                                .height(52.dp)
                                .testTag("space_key")
                        ) {
                            Text("SPACE", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (currentSeq.isNotEmpty()) {
                                    currentSeq = currentSeq.dropLast(1)
                                } else if (typedText.isNotEmpty()) {
                                    typedText = typedText.dropLast(1)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("backspace_key")
                        ) {
                            Text("⌫", fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class KeyInfo(val digit: String, val enLabel: String, val bnLabel: String)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadButton(
    digit: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 2.dp,
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("keypad_btn_$digit")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(text = digit, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DictionaryTab(viewModel: T9ViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val words by viewModel.allWords.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dictionary_search_input"),
            label = { Text("Search Dictionary (Word or T9 digits)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Total Mapped Words: ${words.size}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (words.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No words found in T9 dictionary. Tap '+' to add a custom word!", textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(words) { item ->
                    WordItemCard(wordItem = item, onDelete = { viewModel.deleteWord(item) })
                }
            }
        }
    }
}

@Composable
fun WordItemCard(wordItem: T9Word, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = wordItem.word, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (wordItem.language == "BN") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = wordItem.language,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "T9 Code: ${wordItem.sequence}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Word", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun GeminiAiTab(viewModel: T9ViewModel) {
    var promptText by remember { mutableStateOf("আজকে আবহাওয়া কেমন?") }
    var senderName by remember { mutableStateOf("কামাল") }
    var senderMessage by remember { mutableStateOf("তুমি কি এখন ফ্রি আছো?") }
    var isBangla by remember { mutableStateOf(true) }

    val aiCompletions by viewModel.aiCompletions.collectAsState()
    val smartReply by viewModel.smartReplyResult.collectAsState()
    val isLoading by viewModel.isAiLoading.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini AI Keyboard Assistant", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Generates real-time next-word completions and polite smart replies in Bengali and English directly while typing.",
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Section 1: Next-Word Completion
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1. Next-Word Prediction", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        label = { Text("Typed Context") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.simulateAiCompletion(promptText, isBangla) },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("Generate AI Predictions ✨")
                        }
                    }

                    if (aiCompletions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Completions:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(aiCompletions) { item ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = item,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Smart Reply Simulator
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("2. Smart Reply Generator", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = senderName,
                        onValueChange = { senderName = it },
                        label = { Text("Sender Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = senderMessage,
                        onValueChange = { senderMessage = it },
                        label = { Text("Received Message") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.simulateSmartReply(senderName, senderMessage, isBangla) },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Generate Smart Reply 💬")
                    }

                    if (smartReply.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Generated Reply:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(smartReply, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddWordDialog(viewModel: T9ViewModel, onDismiss: () -> Unit) {
    var wordInput by remember { mutableStateOf("") }
    var langInput by remember { mutableStateOf("BN") }
    var seqInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Add Custom T9 Word", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                OutlinedTextField(
                    value = wordInput,
                    onValueChange = {
                        wordInput = it
                        seqInput = viewModel.calculateT9Sequence(it)
                    },
                    label = { Text("Word (e.g., পড়া / Bangla)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            langInput = "BN"
                            seqInput = viewModel.calculateT9Sequence(wordInput)
                        },
                        colors = if (langInput == "BN") ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("Bangla (BN)")
                    }

                    OutlinedButton(
                        onClick = {
                            langInput = "EN"
                            seqInput = viewModel.calculateT9Sequence(wordInput)
                        },
                        colors = if (langInput == "EN") ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("English (EN)")
                    }
                }

                OutlinedTextField(
                    value = seqInput,
                    onValueChange = { seqInput = it },
                    label = { Text("T9 Digits Sequence (e.g. 2627)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (wordInput.isNotBlank() && seqInput.isNotBlank()) {
                                viewModel.addWord(seqInput, wordInput, langInput)
                                onDismiss()
                            }
                        }
                    ) {
                        Text("Save Word")
                    }
                }
            }
        }
    }
}
