package com.example.engine

import com.example.data.dictionary.BanglaDictionary
import com.example.data.dictionary.BengaliTrieDictionary
import com.example.data.dictionary.CandidateResult

data class PredictionResult(
    val candidates: List<String>,
    val nextKeySuggestions: Map<Char, List<Char>> = emptyMap(),
    val isAutoCorrected: Boolean = false
)

class UniversalDisambiguator(
    private val trieDictionary: BengaliTrieDictionary = BengaliTrieDictionary()
) {

    // Common Yuktakhor (compound consonant) bindings
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
        userCustomWords: List<String> = emptyList()
    ): PredictionResult {
        // Case 1: Empty sequence -> Predict next word from N-Gram Context Graph
        if (currentSequence.isEmpty()) {
            val contextPredictions = predictNextWordFromContext(prevWord)
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

        // Case 3: Fallback mathematical synthesis (Kar & Yuktakhor Binder) if sequence yields no direct Trie match
        if (candidateWords.isEmpty()) {
            val synthesized = synthesizeAutoKarAndYuktakhor(currentSequence)
            if (synthesized.isNotEmpty()) {
                candidateWords.addAll(synthesized)
            }
        }

        val nextKeysMap = calculateNextKeySuggestions(currentSequence, candidateWords)

        return PredictionResult(
            candidates = candidateWords.distinct().take(6),
            nextKeySuggestions = nextKeysMap
        )
    }

    private fun predictNextWordFromContext(prevWord: String?): List<String> {
        if (prevWord.isNullOrBlank()) {
            return listOf("আমি", "আমার", "তুমি", "তোমার", "শুভ", "ধন্যবাদ", "বাংলাদেশ", "আজ")
        }
        val cleanPrev = prevWord.trim()

        val bigramMatches = mutableListOf<String>()
        for (word in BanglaDictionary.builtInWords) {
            val score = trieDictionary.getBigramScore(cleanPrev, word)
            if (score > 0) {
                bigramMatches.add(word)
            }
        }

        if (bigramMatches.isNotEmpty()) {
            return bigramMatches.distinct().take(6)
        }

        return when (cleanPrev) {
            "আমার" -> listOf("সোনার", "দেশ", "নাম", "বাংলাদেশ", "বন্ধু", "মা")
            "আমি" -> listOf("আছি", "যাব", "তোমাকে", "বলব", "কাজ", "বাংলা")
            "তুমি" -> listOf("কেমন", "আছো", "কোথায়", "কী", "যাবে", "বলো")
            "শুভ" -> listOf("সকাল", "রাত্রি", "জন্মদিন", "কামনা", "ঈদ")
            "কেমন" -> listOf("আছো", "আছেন", "আছিস")
            "ধন্যবাদ" -> listOf("তোমাকে", "আপনাকে", "ভাই")
            "বাংলাদেশ" -> listOf("আমার", "আমাদের", "দেশ", "জিন্দাবাদ")
            else -> listOf("এবং", "কিন্তু", "তাহলে", "কাজ", "সুন্দর", "ভালো")
        }
    }

    /**
     * Pure mathematical sequence disambiguation & Kar/Yuktakhor synthesizer
     */
    private fun synthesizeAutoKarAndYuktakhor(sequence: String): List<String> {
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

    private fun calculateNextKeySuggestions(
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

    fun learnSentence(sentence: String) {
        val words = sentence.trim().split("\\s+".toRegex())
        for (i in 0 until words.size - 1) {
            trieDictionary.addBigram(words[i], words[i + 1], weight = 2)
            learnWord(words[i])
        }
        if (words.isNotEmpty()) {
            learnWord(words.last())
        }
    }
}
