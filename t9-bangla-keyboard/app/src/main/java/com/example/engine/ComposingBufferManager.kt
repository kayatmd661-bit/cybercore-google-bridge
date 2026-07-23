package com.example.engine

import android.view.inputmethod.InputConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ComposingState(
    val rawBuffer: String = "",
    val topPrediction: String = "",
    val candidates: List<String> = emptyList(),
    val nextKeySuggestions: Map<Char, List<Char>> = emptyMap()
)

class ComposingBufferManager(
    private val phoneticSolver: DynamicAIPhoneticSolver,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(ComposingState())
    val state: StateFlow<ComposingState> = _state.asStateFlow()

    private var inputConnection: InputConnection? = null
    private var lastCommittedWord: String? = null
    private var autoCommitJob: Job? = null

    var onWordCommitted: ((String) -> Unit)? = null

    fun setInputConnection(ic: InputConnection?) {
        this.inputConnection = ic
    }

    fun onKeyPress(
        keyChar: Char,
        userCustomWords: List<String> = emptyList(),
        localContextPairs: List<Pair<String, String>> = emptyList()
    ) {
        cancelAutoCommitTimer()

        val newBuffer = _state.value.rawBuffer + keyChar
        updateBufferAndPredict(newBuffer, userCustomWords, localContextPairs)

        scheduleAutoCommitTimer(userCustomWords, localContextPairs)
    }

    fun onExplicitChar(
        charStr: String,
        userCustomWords: List<String> = emptyList(),
        localContextPairs: List<Pair<String, String>> = emptyList()
    ) {
        cancelAutoCommitTimer()

        val newBuffer = _state.value.rawBuffer + charStr
        updateBufferAndPredict(newBuffer, userCustomWords, localContextPairs)

        scheduleAutoCommitTimer(userCustomWords, localContextPairs)
    }

    fun onBackspace(
        userCustomWords: List<String> = emptyList(),
        localContextPairs: List<Pair<String, String>> = emptyList()
    ) {
        cancelAutoCommitTimer()

        val current = _state.value.rawBuffer
        if (current.isNotEmpty()) {
            val newBuffer = current.dropLast(1)
            updateBufferAndPredict(newBuffer, userCustomWords, localContextPairs)
        } else {
            // Delete committed character before the cursor
            inputConnection?.deleteSurroundingText(1, 0)
        }
    }

    fun updateBufferAndPredict(
        newBuffer: String,
        userCustomWords: List<String> = emptyList(),
        localContextPairs: List<Pair<String, String>> = emptyList()
    ) {
        val predictionResult = phoneticSolver.predict(
            currentSequence = newBuffer,
            prevWord = lastCommittedWord,
            userCustomWords = userCustomWords,
            localContextPairs = localContextPairs
        )

        val topWord = if (predictionResult.candidates.isNotEmpty()) {
            predictionResult.candidates.first()
        } else {
            newBuffer
        }

        _state.value = ComposingState(
            rawBuffer = newBuffer,
            topPrediction = topWord,
            candidates = predictionResult.candidates,
            nextKeySuggestions = predictionResult.nextKeySuggestions
        )

        // Show real-time composing text (underlined/highlighted)
        if (newBuffer.isNotEmpty()) {
            inputConnection?.setComposingText(topWord, 1)
        } else {
            inputConnection?.finishComposingText()
        }
    }

    fun commitCurrent(
        chosenWord: String? = null,
        autoSpace: Boolean = true
    ) {
        cancelAutoCommitTimer()

        val wordToCommit = chosenWord
            ?: _state.value.topPrediction.ifEmpty { _state.value.rawBuffer }

        if (wordToCommit.isNotBlank()) {
            // Commit text to target field
            inputConnection?.commitText(wordToCommit, 1)

            if (autoSpace) {
                inputConnection?.commitText(" ", 1)
            }

            phoneticSolver.learnWord(wordToCommit)
            lastCommittedWord = wordToCommit
            onWordCommitted?.invoke(wordToCommit)
        }

        flushBuffer()
    }

    fun flushBuffer() {
        cancelAutoCommitTimer()
        _state.value = ComposingState()
        inputConnection?.finishComposingText()
    }

    private fun scheduleAutoCommitTimer(
        userCustomWords: List<String>,
        localContextPairs: List<Pair<String, String>>
    ) {
        autoCommitJob = scope.launch {
            delay(800) // 800ms typing pause auto-commit
            if (_state.value.rawBuffer.isNotEmpty()) {
                commitCurrent(autoSpace = true)
            }
        }
    }

    private fun cancelAutoCommitTimer() {
        autoCommitJob?.cancel()
        autoCommitJob = null
    }
}
