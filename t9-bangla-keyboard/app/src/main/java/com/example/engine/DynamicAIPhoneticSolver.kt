package com.example.engine

import com.example.data.dictionary.BanglaDictionary
import com.example.data.dictionary.BengaliTrieDictionary
import com.example.data.dictionary.CandidateResult

class DynamicAIPhoneticSolver(
    private val trieDictionary: BengaliTrieDictionary = BengaliTrieDictionary()
) {

    private val yuktakhorMap = mapOf(
        "কষ" to "ক্ষ", "কক" to "ক্ক", "কত" to "ক্ত", "কস" to "ক্স",
        "গগ" to "জ্ঞ", "জঞ" to "জ্ঞ", "ঞজ" to "ঞ্জ",
        "তত" to "ত্ত", "তথ" to "ত্থ", "তবা" to "ত্ব",
        "নদ" to "ন্দ", "নত" to "ন্ত", "নথ" to "ন্থ", "নধ" to "ন্ধ", "নন" to "ন্ন",
        "পব" to "প্ব", "পত" to "প্ত",
        "বব" to "ব্ব", "বদ" to "ব্দ", "বধ" to "ব্ধ",
        "মপ" to "ম্প", "মব" to "ম্ব", "মভ" to "ম্ভ", "মম" to "ম্ম",
        "সত" to "স্ত", "সথ" to "স্থ", "সপ" to "স্প", "সট" to "স্ট",
        "শর" to "শ্র", "সর" to "স্র", "কর" to "ক্র", "গর" to "গ্র", "তর" to "ত্র", "প্র" to "প্র", "ধর" to "ধ্র", "বর" to "ব্র"
    )

    fun predict(
        currentSequence: String,
        prevWord: String? = null,
        userCustomWords: List<String> = emptyList(),
        localContextPairs: List<Pair<String, String>> = emptyList()
    ): PredictionResult {
        // Case 1: Empty sequence -> Predict next word using local context memory graph + n-grams
        if (currentSequence.isEmpty()) {
            val contextPredictions = predictNextWordFromContext(prevWord, localContextPairs)
            return PredictionResult(
                candidates = contextPredictions,
                nextKeySuggestions = getDefaultNextKeys()
            )
        }

        // Case 2: Sequence lookup in Trie
        val candidateResults = trieDictionary.searchBySequence(
            sequence = currentSequence,
            prevWord = prevWord,
            userCustomWords = userCustomWords,
            maxResults = 10
        )

        val candidateWords = candidateResults.map { it.word }.toMutableList()

        // Case 3: Fallback mathematical synthesis (Automatic Phonetic Kar & Yuktakhor Synthesis)
        if (candidateWords.isEmpty()) {
            val synthesized = synthesizePhoneticKarAndYuktakhor(currentSequence)
            if (synthesized.isNotEmpty()) {
                candidateWords.addAll(synthesized)
            }
        }

        val nextKeysMap = calculateDynamicNextKeys(currentSequence, candidateWords)

        return PredictionResult(
            candidates = candidateWords.distinct().take(6),
            nextKeySuggestions = nextKeysMap
        )
    }

    private fun predictNextWordFromContext(
        prevWord: String?,
        localContextPairs: List<Pair<String, String>>
    ): List<String> {
        val matches = mutableListOf<String>()

        if (!prevWord.isNullOrBlank()) {
            val cleanPrev = prevWord.trim()

            // Check local SQLite context history first
            for (pair in localContextPairs) {
                if (pair.first == cleanPrev && !matches.contains(pair.second)) {
                    matches.add(pair.second)
                }
            }

            // Check N-Gram bigram dictionary map
            for (word in BanglaDictionary.builtInWords) {
                val score = trieDictionary.getBigramScore(cleanPrev, word)
                if (score > 0 && !matches.contains(word)) {
                    matches.add(word)
                }
            }
        }

        if (matches.isNotEmpty()) {
            return matches.distinct().take(6)
        }

        // Contextual default fallback
        return when (prevWord?.trim()) {
            "আমার" -> listOf("সোনার", "দেশ", "নাম", "বাংলাদেশ", "বন্ধু", "মা")
            "আমি" -> listOf("আছি", "যাব", "তোমাকে", "বলব", "কাজ", "বাংলা")
            "তুমি" -> listOf("কেমন", "আছো", "কোথায়", "কী", "যাবে", "বলো")
            "শুভ" -> listOf("সকাল", "রাত্রি", "জন্মদিন", "কামনা", "ঈদ")
            "কেমন" -> listOf("আছো", "আছেন", "আছিস")
            "ধন্যবাদ" -> listOf("তোমাকে", "আপনাকে", "ভাই")
            "বাংলাদেশ" -> listOf("আমার", "আমাদের", "দেশ", "জিন্দাবাদ")
            else -> listOf("আমি", "আমার", "তুমি", "তোমার", "শুভ", "ধন্যবাদ", "বাংলাদেশ", "আজ")
        }
    }

    /**
     * Automatic Phonetic Kar & Compound Consonant (Yuktakhor) Synthesizer Engine
     */
    private fun synthesizePhoneticKarAndYuktakhor(sequence: String): List<String> {
        if (sequence.isEmpty()) return emptyList()

        val results = mutableListOf<String>()
        val sbPrimary = StringBuilder()
        val sbKarBound = StringBuilder()

        var lastChar: Char? = null

        for (i in sequence.indices) {
            val key = sequence[i]
            val keyChars = BanglaDictionary.keyCharMap[key] ?: listOf(key)
            val primaryChar = keyChars.first()

            if (i == 0) {
                sbPrimary.append(primaryChar)
                sbKarBound.append(primaryChar)
                lastChar = primaryChar
            } else {
                sbPrimary.append(primaryChar)

                val pair = "${lastChar}${primaryChar}"
                val jukto = yuktakhorMap[pair]
                if (jukto != null) {
                    if (sbKarBound.isNotEmpty()) {
                        sbKarBound.deleteCharAt(sbKarBound.length - 1)
                        sbKarBound.append(jukto)
                    }
                } else {
                    sbKarBound.append(primaryChar)
                }
                lastChar = primaryChar
            }
        }

        if (sbKarBound.isNotEmpty()) results.add(sbKarBound.toString())
        if (sbPrimary.isNotEmpty() && sbPrimary.toString() != sbKarBound.toString()) {
            results.add(sbPrimary.toString())
        }

        return results
    }

    /**
     * Dynamic Keyboard Button Re-rendering & Cycle Engine calculation
     */
    private fun calculateDynamicNextKeys(
        currentSeq: String,
        candidateWords: List<String>
    ): Map<Char, List<Char>> {
        val resultMap = mutableMapOf<Char, MutableList<Char>>()

        for (k in '1'..'9') {
            resultMap[k] = mutableListOf()
        }

        val currentLen = currentSeq.length
        for (word in candidateWords) {
            if (word.length > currentLen) {
                val nextChar = word[currentLen]
                val targetKey = BanglaDictionary.getT9KeyForChar(nextChar)
                val list = resultMap[targetKey]
                if (list != null && !list.contains(nextChar) && list.size < 3) {
                    list.add(nextChar)
                }
            }
        }

        for (k in '1'..'9') {
            val list = resultMap[k]!!
            if (list.isEmpty()) {
                val defaults = BanglaDictionary.keyCharMap[k] ?: emptyList()
                list.addAll(defaults.take(3))
            }
        }

        return resultMap
    }

    private fun getDefaultNextKeys(): Map<Char, List<Char>> {
        val map = mutableMapOf<Char, List<Char>>()
        for (k in '1'..'9') {
            map[k] = BanglaDictionary.keyCharMap[k]?.take(3) ?: emptyList()
        }
        return map
    }

    fun learnWord(word: String) {
        if (word.trim().length > 1) {
            trieDictionary.insert(word.trim(), frequency = 50)
        }
    }
}
