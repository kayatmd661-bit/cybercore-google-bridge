package com.example.t9keyboard

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.example.t9keyboard.ai.GeminiManager
import com.example.t9keyboard.data.AppDatabase
import com.example.t9keyboard.data.T9Word
import com.example.t9keyboard.logic.T9Engine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class KeyboardMode { T9, NUM, TEXT }

class T9KeyboardService : InputMethodService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val currentInputSequence = StringBuilder()
    private var isBanglaMode = true
    private var currentMode = KeyboardMode.T9
    private var speechRecognizer: SpeechRecognizer? = null

    private lateinit var mainRootLayout: LinearLayout
    private lateinit var suggestionContainer: LinearLayout
    private lateinit var btnLang: Button
    private lateinit var btnMode: Button
    private lateinit var btnVoice: Button
    private lateinit var btnGemini: Button
    private val keyButtons = mutableMapOf<Char, Button>()

    private val db by lazy { AppDatabase.getDatabase(applicationContext) }

    companion object {
        @Volatile
        var instance: T9KeyboardService? = null
            private set

        fun generateAndSendReply(
            sender: String,
            message: String,
            userCustomInput: String?,
            onReplyGenerated: (String) -> Unit
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val reply = GeminiManager.generateSmartReply(sender, message, userCustomInput, true)
                withContext(Dispatchers.Main) {
                    instance?.currentInputConnection?.commitText(reply, 1)
                    onReplyGenerated(reply)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        setupVoiceRecognizer()
    }

    override fun onCreateInputView(): View {
        mainRootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F5FA"))
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(8))
        }

        // --- Top Bar (Suggestions + Voice + AI) ---
        val topBar = RelativeLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(48)
            )
            setBackgroundColor(Color.WHITE)
            elevation = dpToPx(2).toFloat()
        }

        // Gemini AI Button
        btnGemini = Button(this).apply {
            id = View.generateViewId()
            text = "✨ AI"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#673AB7"))
            layoutParams = RelativeLayout.LayoutParams(
                dpToPx(60),
                RelativeLayout.LayoutParams.MATCH_PARENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
            }
            setOnClickListener { generateGeminiSuggestions() }
        }
        topBar.addView(btnGemini)

        // Keyboard Mode Switcher Button (T9 / 123 / ABC)
        btnMode = Button(this).apply {
            id = View.generateViewId()
            text = "T9"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1D192B"))
            setBackgroundColor(Color.parseColor("#E8DEF8"))
            layoutParams = RelativeLayout.LayoutParams(
                dpToPx(56),
                RelativeLayout.LayoutParams.MATCH_PARENT
            ).apply {
                addRule(RelativeLayout.LEFT_OF, btnGemini.id)
            }
            setOnClickListener { toggleMode() }
        }
        topBar.addView(btnMode)

        // Voice Typing Button
        btnVoice = Button(this).apply {
            id = View.generateViewId()
            text = "🎙️"
            textSize = 16f
            setTextColor(Color.parseColor("#1A1C1E"))
            setBackgroundColor(Color.parseColor("#E0E2EC"))
            layoutParams = RelativeLayout.LayoutParams(
                dpToPx(48),
                RelativeLayout.LayoutParams.MATCH_PARENT
            ).apply {
                addRule(RelativeLayout.LEFT_OF, btnMode.id)
            }
            setOnClickListener { startVoiceListening() }
        }
        topBar.addView(btnVoice)

        // Candidate Suggestions Scroll Container
        val scroll = HorizontalScrollView(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            ).apply {
                addRule(RelativeLayout.LEFT_OF, btnVoice.id)
            }
            isHorizontalScrollBarEnabled = false
        }

        suggestionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), 0, dpToPx(12), 0)
        }
        scroll.addView(suggestionContainer)
        topBar.addView(scroll)

        mainRootLayout.addView(topBar)

        // --- 3x4 Keypad Grid ---
        val gridLayout = GridLayout(this).apply {
            columnCount = 3
            rowCount = 4
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(6)
            }
        }

        val buttonConfigs = listOf(
            ButtonConfig(".,?\n1", "অআ\n১"),
            ButtonConfig("ABC\n2", "কখ\n২"),
            ButtonConfig("DEF\n3", "চছ\n৩"),
            ButtonConfig("GHI\n4", "টঠ\n৪"),
            ButtonConfig("JKL\n5", "তথ\n৫"),
            ButtonConfig("MNO\n6", "পফ\n৬"),
            ButtonConfig("PQRS\n7", "যর\n৭"),
            ButtonConfig("TUV\n8", "ষস\n৮"),
            ButtonConfig("WXYZ\n9", "াি\n৯"),
            ButtonConfig("BN", "BN"),
            ButtonConfig("Space", "Space"),
            ButtonConfig("⌫", "⌫")
        )

        for (i in buttonConfigs.indices) {
            val config = buttonConfigs[i]
            val btn = Button(this).apply {
                text = if (isBanglaMode) config.banglaText else config.englishText
                textSize = 15f
                isAllCaps = false
                setTextColor(Color.parseColor("#1B1B1F"))
                setBackgroundColor(Color.WHITE)
                elevation = dpToPx(1).toFloat()

                val rowSpec = GridLayout.spec(i / 3)
                val colSpec = GridLayout.spec(i % 3, 1.0f)
                layoutParams = GridLayout.LayoutParams(rowSpec, colSpec).apply {
                    width = 0
                    height = dpToPx(60)
                    setMargins(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3))
                }
            }

            val keyText = config.englishText
            if (keyText == "BN" || keyText == "EN") {
                btnLang = btn.apply {
                    setBackgroundColor(Color.parseColor("#E0E2EC"))
                    setOnClickListener { toggleLanguage() }
                }
            } else if (keyText == "Space") {
                btn.setOnClickListener { handleSpaceKey() }
            } else if (keyText == "⌫") {
                btn.apply {
                    setBackgroundColor(Color.parseColor("#F2B8B5"))
                    setTextColor(Color.parseColor("#601410"))
                    setOnClickListener { handleBackSpace() }
                }
            } else {
                val digit = (i + 1).toString()
                keyButtons[digit.first()] = btn
                btn.setOnClickListener { handleKeyInput(digit) }
                btn.setOnLongClickListener {
                    val numChar = getDigitChar(digit, isBanglaMode)
                    currentInputConnection?.commitText(numChar, 1)
                    resetInput()
                    Toast.makeText(this@T9KeyboardService, "Digit: $numChar", Toast.LENGTH_SHORT).show()
                    true
                }
            }

            gridLayout.addView(btn)
        }

        mainRootLayout.addView(gridLayout)
        return mainRootLayout
    }

    private data class ButtonConfig(val englishText: String, val banglaText: String)

    private fun handleKeyInput(num: String) {
        val ic = currentInputConnection ?: return

        when (currentMode) {
            KeyboardMode.NUM -> {
                val numChar = getDigitChar(num, isBanglaMode)
                ic.commitText(numChar, 1)
            }
            KeyboardMode.T9 -> {
                currentInputSequence.append(num)
                showWordSuggestions()
            }
            KeyboardMode.TEXT -> {
                val chars = T9Engine.getCharMappings(num.first(), isBanglaMode)
                suggestionContainer.removeAllViews()
                for (ch in chars) {
                    addPillToSuggestionContainer(ch) {
                        ic.commitText(ch, 1)
                        resetInput()
                    }
                }
                if (chars.isNotEmpty()) {
                    ic.commitText(chars.first(), 1)
                }
            }
        }
    }

    private fun updateKeypadLabels() {
        val seq = currentInputSequence.toString()
        val contextSentence = currentInputConnection?.getTextBeforeCursor(100, 0)?.toString() ?: ""
        val labels = T9Engine.getDynamicKeyLabels(seq, isBanglaMode, contextSentence)
        for ((d, btn) in keyButtons) {
            val labelText = labels[d] ?: ""
            val digitDisp = if (isBanglaMode) getDigitChar(d.toString(), true) else d.toString()
            btn.text = "$labelText\n$digitDisp"
        }
    }

    private fun showWordSuggestions() {
        suggestionContainer.removeAllViews()
        val seq = currentInputSequence.toString()
        val contextSentence = currentInputConnection?.getTextBeforeCursor(100, 0)?.toString() ?: ""

        if (seq.isEmpty()) {
            currentInputConnection?.finishComposingText()
            updateKeypadLabels()

            val nextWordPredictions = T9Engine.getNextWordPredictions(contextSentence, isBanglaMode)
            if (nextWordPredictions.isNotEmpty()) {
                val prevWord = contextSentence.trim().split("\\s+".toRegex()).lastOrNull() ?: ""
                for (nextWord in nextWordPredictions) {
                    addPillToSuggestionContainer(nextWord) {
                        if (prevWord.isNotEmpty()) {
                            T9Engine.recordWordPair(prevWord, nextWord)
                        }
                        currentInputConnection?.commitText(nextWord + " ", 1)
                        resetInput()
                    }
                }
            }
            return
        }

        serviceScope.launch(Dispatchers.IO) {
            val suggestions = T9Engine.getSuggestions(db.t9WordDao(), seq, isBanglaMode, contextSentence)

            withContext(Dispatchers.Main) {
                suggestionContainer.removeAllViews()
                updateKeypadLabels()

                val prevWord = contextSentence.trim().split("\\s+".toRegex()).lastOrNull() ?: ""

                if (suggestions.isNotEmpty()) {
                    val topCandidate = suggestions.first()
                    currentInputConnection?.setComposingText(topCandidate, 1)

                    for (candidate in suggestions) {
                        addPillToSuggestionContainer(candidate) {
                            if (prevWord.isNotEmpty()) {
                                T9Engine.recordWordPair(prevWord, candidate)
                            }
                            currentInputConnection?.commitText(candidate + " ", 1)
                            resetInput()
                        }
                    }
                } else {
                    currentInputConnection?.setComposingText(seq, 1)
                    addPillToSuggestionContainer(seq) {
                        if (prevWord.isNotEmpty()) {
                            T9Engine.recordWordPair(prevWord, seq)
                        }
                        currentInputConnection?.commitText(seq, 1)
                        resetInput()
                    }
                }
            }
        }
    }

    private fun addPillToSuggestionContainer(text: String, onClick: () -> Unit) {
        val tv = TextView(this).apply {
            this.text = text
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1B1B1F"))
            setBackgroundColor(Color.parseColor("#E8DEF8"))
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(4), 0, dpToPx(4), 0)
            }
            layoutParams = params
            setOnClickListener { onClick() }
        }
        suggestionContainer.addView(tv)
    }

    private fun handleBackSpace() {
        if (currentInputSequence.isNotEmpty()) {
            currentInputSequence.deleteCharAt(currentInputSequence.length - 1)
            showWordSuggestions()
        } else {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
    }

    private fun handleSpaceKey() {
        val ic = currentInputConnection ?: return
        val contextSentence = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""

        if (currentInputSequence.isNotEmpty()) {
            val seq = currentInputSequence.toString()
            serviceScope.launch(Dispatchers.IO) {
                val suggestions = T9Engine.getSuggestions(db.t9WordDao(), seq, isBanglaMode, contextSentence)
                val topCandidate = suggestions.firstOrNull() ?: seq

                val prevWord = contextSentence.trim().split("\\s+".toRegex()).lastOrNull() ?: ""
                if (prevWord.isNotEmpty()) {
                    T9Engine.recordWordPair(prevWord, topCandidate)
                }

                withContext(Dispatchers.Main) {
                    ic.commitText("$topCandidate ", 1)
                    resetInput()
                }
            }
        } else {
            ic.commitText(" ", 1)
            showWordSuggestions()
        }
    }

    private fun toggleLanguage() {
        isBanglaMode = !isBanglaMode
        btnLang.text = if (isBanglaMode) "BN" else "EN"
        btnLang.setBackgroundColor(if (isBanglaMode) Color.parseColor("#E0E2EC") else Color.parseColor("#D0BCFF"))
        resetInput()
        Toast.makeText(this, if (isBanglaMode) "বাংলা মোড" else "English Mode", Toast.LENGTH_SHORT).show()
    }

    private fun toggleMode() {
        currentMode = when (currentMode) {
            KeyboardMode.T9 -> KeyboardMode.NUM
            KeyboardMode.NUM -> KeyboardMode.TEXT
            KeyboardMode.TEXT -> KeyboardMode.T9
        }
        btnMode.text = when (currentMode) {
            KeyboardMode.T9 -> "T9"
            KeyboardMode.NUM -> "123"
            KeyboardMode.TEXT -> "ABC"
        }
        resetInput()
        val modeMsg = when (currentMode) {
            KeyboardMode.T9 -> "Predictive T9 Mode"
            KeyboardMode.NUM -> "Numeric Mode (123)"
            KeyboardMode.TEXT -> "Standard Text Mode (ABC)"
        }
        Toast.makeText(this, modeMsg, Toast.LENGTH_SHORT).show()
    }

    private fun getDigitChar(digit: String, isBangla: Boolean): String {
        if (!isBangla) return digit
        return when (digit) {
            "1" -> "১"; "2" -> "২"; "3" -> "৩"; "4" -> "৪"
            "5" -> "৫"; "6" -> "৬"; "7" -> "৭"; "8" -> "৮"
            "9" -> "৯"; "0" -> "০"
            else -> digit
        }
    }

    private fun resetInput() {
        currentInputSequence.setLength(0)
        currentInputConnection?.finishComposingText()
        suggestionContainer.removeAllViews()
        updateKeypadLabels()
    }

    private fun generateGeminiSuggestions() {
        val ic = currentInputConnection ?: return
        val et = ic.getExtractedText(ExtractedTextRequest(), 0)
        val currentText = et?.text?.toString()?.trim() ?: ""

        btnGemini.text = "⏳"

        serviceScope.launch {
            val suggestions = GeminiManager.generateWordCompletions(currentText, isBanglaMode)
            btnGemini.text = "✨ AI"
            suggestionContainer.removeAllViews()

            for (sugg in suggestions) {
                val cleanSugg = sugg.trim()
                if (cleanSugg.isNotEmpty()) {
                    val tv = TextView(this@T9KeyboardService).apply {
                        text = cleanSugg
                        textSize = 15f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(Color.parseColor("#673AB7"))
                        setBackgroundColor(Color.parseColor("#F3EDF7"))
                        setPadding(dpToPx(14), dpToPx(6), dpToPx(14), dpToPx(6))
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(dpToPx(4), 0, dpToPx(4), 0)
                        }
                        layoutParams = params
                        setOnClickListener {
                            ic.commitText("$cleanSugg ", 1)
                            resetInput()
                        }
                    }
                    suggestionContainer.addView(tv)
                }
            }
        }
    }

    private fun setupVoiceRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    btnVoice.text = "🛑"
                    Toast.makeText(this@T9KeyboardService, "কথা বলুন / Speak now...", Toast.LENGTH_SHORT).show()
                }

                override fun onResults(results: Bundle?) {
                    btnVoice.text = "🎙️"
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        currentInputConnection?.commitText("${matches[0]} ", 1)
                    }
                }

                override fun onError(error: Int) {
                    btnVoice.text = "🎙️"
                    Toast.makeText(this@T9KeyboardService, "ভয়েস ইনপুট পুনরায় চেষ্টা করুন", Toast.LENGTH_SHORT).show()
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startVoiceListening() {
        if (speechRecognizer == null) {
            setupVoiceRecognizer()
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isBanglaMode) "bn-BD" else Locale.ENGLISH.toString())
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice recognition error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        serviceScope.cancel()
        if (instance == this) instance = null
    }
}
