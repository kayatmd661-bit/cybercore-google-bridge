package com.example.t9keyboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.t9keyboard.ai.GeminiManager
import com.example.t9keyboard.data.AppDatabase
import com.example.t9keyboard.data.T9Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.t9keyboard.logic.T9Engine

class T9ViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.t9WordDao()

    val searchQuery = MutableStateFlow("")
    val allWords: StateFlow<List<T9Word>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                dao.getAllWordsFlow()
            } else {
                dao.searchWords(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Simulation State
    private val _aiCompletions = MutableStateFlow<List<String>>(emptyList())
    val aiCompletions: StateFlow<List<String>> = _aiCompletions.asStateFlow()

    private val _smartReplyResult = MutableStateFlow("")
    val smartReplyResult: StateFlow<String> = _smartReplyResult.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    fun addWord(sequence: String, word: String, language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val isBangla = language == "BN"
            val calculatedSeq = if (sequence.isBlank()) T9Engine.textToT9Sequence(word.trim(), isBangla) else sequence.trim()
            val t9Word = T9Word(
                sequence = calculatedSeq,
                word = word.trim(),
                language = language,
                frequency = 500
            )
            dao.insertWord(t9Word)
        }
    }

    fun deleteWord(word: T9Word) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteWord(word)
        }
    }

    fun simulateAiCompletion(text: String, isBangla: Boolean) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            val results = GeminiManager.generateWordCompletions(text, isBangla)
            _aiCompletions.value = results
            _isAiLoading.value = false
        }
    }

    fun simulateSmartReply(sender: String, message: String, isBangla: Boolean) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val reply = GeminiManager.generateSmartReply(sender, message, null, isBangla)
            _smartReplyResult.value = reply
            _isAiLoading.value = false
        }
    }

    /**
     * Converts Bangla or English character input into corresponding T9 Keypad Sequence
     */
    fun calculateT9Sequence(text: String, isBangla: Boolean = true): String {
        return T9Engine.textToT9Sequence(text, isBangla)
    }

    suspend fun getSuggestions(seq: String, isBanglaMode: Boolean, contextSentence: String = ""): List<String> {
        return T9Engine.getSuggestions(dao, seq, isBanglaMode, contextSentence)
    }
}
