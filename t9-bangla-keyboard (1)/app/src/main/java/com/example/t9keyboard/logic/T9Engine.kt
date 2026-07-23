package com.example.t9keyboard.logic

import com.example.t9keyboard.data.T9Word
import com.example.t9keyboard.data.T9WordDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrieNode {
    val children = mutableMapOf<Char, TrieNode>()
    val exactWords = mutableListOf<Pair<String, Int>>() // word to frequency
    val prefixWords = mutableListOf<Pair<String, Int>>() // word to frequency
}

class T9Trie {
    val root = TrieNode()

    fun insert(word: String, sequence: String, frequency: Int) {
        if (sequence.isEmpty() || word.isEmpty()) return
        var current = root
        for (ch in sequence) {
            current = current.children.getOrPut(ch) { TrieNode() }
            if (current.prefixWords.none { it.first == word }) {
                current.prefixWords.add(Pair(word, frequency))
            }
        }
        if (current.exactWords.none { it.first == word }) {
            current.exactWords.add(Pair(word, frequency))
        }
    }

    fun getExactMatches(sequence: String): List<Pair<String, Int>> {
        var current = root
        for (ch in sequence) {
            current = current.children[ch] ?: return emptyList()
        }
        return current.exactWords.sortedByDescending { it.second }
    }

    fun getPrefixMatches(sequence: String): List<Pair<String, Int>> {
        var current = root
        for (ch in sequence) {
            current = current.children[ch] ?: return emptyList()
        }
        val exacts = current.exactWords.map { it.first }.toSet()
        return current.prefixWords
            .filter { !exacts.contains(it.first) }
            .sortedByDescending { it.second }
    }

    fun clear() {
        root.children.clear()
        root.exactWords.clear()
        root.prefixWords.clear()
    }
}

object T9Engine {

    private val bnTrie = T9Trie()
    private val enTrie = T9Trie()
    @Volatile private var isTriePopulated = false

    private val dynamicBigramMap = mutableMapOf<String, MutableMap<String, Int>>()

    fun recordWordPair(prevWord: String, currentWord: String) {
        if (prevWord.isEmpty() || currentWord.isEmpty()) return
        val cleanPrev = prevWord.trim().lowercase()
        val cleanCurr = currentWord.trim()
        val subMap = dynamicBigramMap.getOrPut(cleanPrev) { mutableMapOf() }
        subMap[cleanCurr] = (subMap[cleanCurr] ?: 0) + 1
    }

    fun getNextWordPredictions(contextSentence: String, isBanglaMode: Boolean): List<String> {
        if (contextSentence.isEmpty()) return emptyList()
        val words = contextSentence.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        val lastWord = words.last().trim()
        val cleanLast = lastWord.lowercase()

        val candidates = mutableListOf<String>()

        // 1. Dynamic Bigram lookup from runtime memory
        val dynamicMatches = dynamicBigramMap[cleanLast]?.entries
            ?.sortedByDescending { it.value }
            ?.map { it.key } ?: emptyList()
        candidates.addAll(dynamicMatches)

        // 2. Trie graph lookup for words matching prefix or related transitions
        val trie = if (isBanglaMode) bnTrie else enTrie
        if (candidates.size < 5 && isTriePopulated) {
            val trieMatches = trie.getPrefixMatches(textToT9Sequence(lastWord, isBanglaMode))
                .map { it.first }
                .filter { it != lastWord }
            candidates.addAll(trieMatches)
        }

        return candidates.distinct().filter { it.isNotEmpty() }
    }

    // English 9-Keypad Map
    val EN_KEY_MAP = mapOf(
        '1' to listOf(".", ",", "?", "!", "1"),
        '2' to listOf("a", "b", "c", "2"),
        '3' to listOf("d", "e", "f", "3"),
        '4' to listOf("g", "h", "i", "4"),
        '5' to listOf("j", "k", "l", "5"),
        '6' to listOf("m", "n", "o", "6"),
        '7' to listOf("p", "q", "r", "s", "7"),
        '8' to listOf("t", "u", "v", "8"),
        '9' to listOf("w", "x", "y", "z", "9"),
        '0' to listOf(" ", "0")
    )

    // Full Bangla Alphabet 9-Keypad Map as specified:
    // Key 1: অ, আ, ই, ঈ, উ, ঊ, ঋ, এ, ঐ, ও, ঔ
    // Key 2: ক, খ, গ, ঘ, ঙ
    // Key 3: চ, ছ, জ, ঝ, ঞ
    // Key 4: ট, ঠ, ড, ঢ, ণ
    // Key 5: ত, থ, দ, ধ, ন, ৎ
    // Key 6: প, ফ, ব, ভ, ম
    // Key 7: য, র, ল, শ
    // Key 8: ষ, স, হ, ড়, ঢ়, য়
    // Key 9: া, ি, ী, ু, ূ, ে, ৈ, ো, ৌ, ্য, ্র, র্, ং, ঃ, ঁ, ্
    val BN_KEY_MAP = mapOf(
        '1' to listOf("অ", "আ", "ই", "ঈ", "উ", "ঊ", "ঋ", "এ", "ঐ", "ও", "ঔ", "১"),
        '2' to listOf("ক", "খ", "গ", "ঘ", "ঙ", "২"),
        '3' to listOf("চ", "ছ", "জ", "ঝ", "ঞ", "৩"),
        '4' to listOf("ট", "ঠ", "ড", "ঢ", "ণ", "৪"),
        '5' to listOf("ত", "থ", "দ", "ধ", "ন", "ৎ", "৫"),
        '6' to listOf("প", "ফ", "ব", "ভ", "ম", "৬"),
        '7' to listOf("য", "র", "ল", "শ", "৭"),
        '8' to listOf("ষ", "স", "হ", "ড়", "ঢ়", "য়", "৮"),
        '9' to listOf("া", "ি", "ী", "ু", "ূ", "ে", "ৈ", "ো", "ৌ", "্য", "্র", "র্", "ং", "ঃ", "ঁ", "্", "৯"),
        '0' to listOf(" ", "০")
    )

    /**
     * Maps individual Bengali / English characters to T9 keypad digits
     */
    fun charToDigit(c: Char, isBangla: Boolean): Char {
        if (!isBangla) {
            return when (c.lowercaseChar()) {
                'a', 'b', 'c' -> '2'
                'd', 'e', 'f' -> '3'
                'g', 'h', 'i' -> '4'
                'j', 'k', 'l' -> '5'
                'm', 'n', 'o' -> '6'
                'p', 'q', 'r', 's' -> '7'
                't', 'u', 'v' -> '8'
                'w', 'x', 'y', 'z' -> '9'
                else -> if (c.isDigit()) c else '2'
            }
        } else {
            val s = c.toString()
            return when {
                s in listOf("অ", "আ", "ই", "ঈ", "উ", "ঊ", "ঋ", "এ", "ঐ", "ও", "ঔ") -> '1'
                s in listOf("ক", "খ", "গ", "ঘ", "ঙ") -> '2'
                s in listOf("চ", "ছ", "জ", "ঝ", "ঞ") -> '3'
                s in listOf("ট", "ঠ", "ড", "ঢ", "ণ") -> '4'
                s in listOf("ত", "থ", "দ", "ধ", "ন", "ৎ") -> '5'
                s in listOf("প", "ফ", "ব", "ভ", "ম") -> '6'
                s in listOf("য", "র", "ল", "শ") -> '7'
                s in listOf("ষ", "স", "হ", "ড়", "ঢ়", "য়") -> '8'
                s in listOf("া", "ি", "ী", "ু", "ূ", "ে", "ৈ", "ো", "ৌ", "্য", "্র", "র্", "ং", "ঃ", "ঁ", "্") -> '9'
                c.isDigit() -> c
                else -> '1'
            }
        }
    }

    /**
     * Converts a complete word into its corresponding T9 digit sequence
     */
    fun textToT9Sequence(text: String, isBangla: Boolean): String {
        val sb = StringBuilder()
        for (c in text) {
            sb.append(charToDigit(c, isBangla))
        }
        return sb.toString()
    }

    fun getCharMappings(digit: Char, isBangla: Boolean): List<String> {
        val map = if (isBangla) BN_KEY_MAP else EN_KEY_MAP
        return map[digit] ?: listOf(digit.toString())
    }

    /**
     * Build or refresh Trie from DB word entities
     */
    fun populateTrie(words: List<T9Word>) {
        bnTrie.clear()
        enTrie.clear()
        for (w in words) {
            val isBn = w.language == "BN"
            val trie = if (isBn) bnTrie else enTrie
            val calculatedSeq = textToT9Sequence(w.word, isBn)
            trie.insert(w.word, calculatedSeq, w.frequency)
        }
        isTriePopulated = true
    }

    /**
     * Master Phonetic Combinatorics Engine for Bengali & English:
     * Generates phonetically valid candidates for unregistered combinations dynamically.
     */
    fun generatePhoneticCombinations(seq: String, isBangla: Boolean, maxCount: Int = 10): List<String> {
        if (seq.isEmpty()) return emptyList()

        if (!isBangla) {
            return generateMultiTapCandidates(seq, isBangla = false, maxCandidates = maxCount)
        }

        // Bangla Phonetic Combinatorics logic
        val keyChars = seq.map { digit ->
            getCharMappings(digit, isBangla = true).filter { !it.first().isDigit() && it != " " }
        }

        if (keyChars.any { it.isEmpty() }) return listOf(seq)

        val results = mutableSetOf<String>()

        // Generate combinations considering vowels, consonants, kars, and yuktakhor
        fun backtrack(index: Int, current: String) {
            if (results.size >= maxCount * 2) return
            if (index == seq.length) {
                results.add(current)
                return
            }

            val options = keyChars[index]
            for (opt in options.take(4)) {
                // Check if binding a Kar (Key 9) after a consonant
                if (opt in listOf("া", "ি", "ী", "ু", "ূ", "ে", "ৈ", "ো", "ৌ", "্য", "্র", "র্")) {
                    if (current.isNotEmpty() && !isBanglaVowel(current.last())) {
                        backtrack(index + 1, current + opt)
                    } else {
                        // Fallback to independent vowel form if preceded by vowel or start
                        val vowelForm = karToVowel(opt)
                        backtrack(index + 1, current + vowelForm)
                    }
                } else {
                    backtrack(index + 1, current + opt)
                }
            }
        }

        backtrack(0, "")

        // Also add conjunct (Yuktakhor) variants if 2 consonants appear consecutively
        if (seq.length >= 2) {
            val conjuncts = generateConjunctVariants(seq, keyChars, maxCount)
            results.addAll(conjuncts)
        }

        return results.filter { it.isNotEmpty() }.take(maxCount)
    }

    private fun isBanglaVowel(c: Char): Boolean {
        val s = c.toString()
        return s in listOf("অ", "আ", "ই", "ঈ", "উ", "ঊ", "ঋ", "এ", "ঐ", "ও", "ঔ", "া", "ি", "ী", "ু", "ূ", "ে", "ৈ", "ো", "ৌ")
    }

    private fun karToVowel(kar: String): String {
        return when (kar) {
            "া" -> "আ"; "ি" -> "ই"; "ী" -> "ঈ"; "ু" -> "উ"; "ূ" -> "ঊ"
            "ে" -> "এ"; "ৈ" -> "ঐ"; "ো" -> "ও"; "ৌ" -> "ঔ"
            else -> kar
        }
    }

    private fun generateConjunctVariants(seq: String, keyChars: List<List<String>>, maxCount: Int): List<String> {
        val list = mutableListOf<String>()
        val firstChars = keyChars[0].take(3)
        val secondChars = if (keyChars.size > 1) keyChars[1].take(3) else emptyList()
        val tailSeq = if (keyChars.size > 2) keyChars.subList(2, keyChars.size) else emptyList()

        for (c1 in firstChars) {
            for (c2 in secondChars) {
                if (c1.isNotEmpty() && c2.isNotEmpty() && !isBanglaVowel(c1.last()) && !isBanglaVowel(c2.last())) {
                    val conjunct = "${c1}্$c2"
                    if (tailSeq.isEmpty()) {
                        list.add(conjunct)
                    } else {
                        for (tail in tailSeq.first().take(2)) {
                            list.add(conjunct + tail)
                        }
                    }
                }
            }
        }
        return list
    }

    fun generateMultiTapCandidates(seq: String, isBangla: Boolean, maxCandidates: Int = 8): List<String> {
        if (seq.isEmpty()) return emptyList()

        if (seq.length == 1) {
            return getCharMappings(seq[0], isBangla)
        }

        var results = listOf("")
        val charLists = seq.map { digit ->
            getCharMappings(digit, isBangla).filter { it != " " && !it.first().isDigit() }.take(3)
        }

        for (list in charLists) {
            if (list.isEmpty()) continue
            val nextResults = mutableListOf<String>()
            for (prefix in results) {
                for (ch in list) {
                    nextResults.add(prefix + ch)
                    if (nextResults.size >= maxCandidates) break
                }
                if (nextResults.size >= maxCandidates) break
            }
            results = nextResults
        }

        val candidates = results.toMutableList()
        if (!candidates.contains(seq)) {
            candidates.add(seq)
        }
        return candidates
    }

    /**
     * Main T9 Lookup Engine using Trie & Phonetic Combinatorics:
     * 1. Query Trie for exact matches ordered by frequency DESC
     * 2. Query Trie for prefix matches ordered by frequency DESC
     * 3. Fall back to Room DB if Trie needs sync
     * 4. Generate Phonetic Combinatorics for unregistered inputs
     * 5. Rank and return deduplicated candidates
     */
    suspend fun getSuggestions(
        dao: T9WordDao,
        seq: String,
        isBanglaMode: Boolean,
        contextSentence: String = ""
    ): List<String> = withContext(Dispatchers.IO) {
        if (seq.isEmpty()) {
            return@withContext getNextWordPredictions(contextSentence, isBanglaMode)
        }

        val lang = if (isBanglaMode) "BN" else "EN"

        // Sync Trie if not initialized
        if (!isTriePopulated) {
            val all = dao.getAllWordsSync()
            if (all.isNotEmpty()) {
                populateTrie(all)
            }
        }

        val trie = if (isBanglaMode) bnTrie else enTrie

        // 1. Trie Exact Matches
        val trieExacts = trie.getExactMatches(seq).map { it.first }

        // 2. Trie Prefix Matches
        val triePrefixes = trie.getPrefixMatches(seq).map { it.first }

        // 3. Fallback DB Queries if Trie returned sparse results
        val dbExacts = if (trieExacts.isEmpty()) dao.getWordsForSequence(seq, lang).map { it.word } else emptyList()
        val dbPrefixes = if (triePrefixes.isEmpty()) dao.getWordsStartingWithSequence(seq, lang).map { it.word } else emptyList()

        // 4. Phonetic Combinatorics for dynamic word generation
        val phoneticCandidates = generatePhoneticCombinations(seq, isBanglaMode, maxCount = 8)

        val rawCandidates = (trieExacts + dbExacts + triePrefixes + dbPrefixes + phoneticCandidates)
            .distinct()
            .filter { it.isNotEmpty() }

        val predictedNextWords = getNextWordPredictions(contextSentence, isBanglaMode)

        if (predictedNextWords.isNotEmpty()) {
            val rankedList = rawCandidates.sortedByDescending { cand ->
                var score = 0
                val predIdx = predictedNextWords.indexOf(cand)
                if (predIdx != -1) {
                    score += (1000 - predIdx * 100)
                } else {
                    val prefixMatchIdx = predictedNextWords.indexOfFirst { it.startsWith(cand) || cand.startsWith(it) }
                    if (prefixMatchIdx != -1) {
                        score += (500 - prefixMatchIdx * 50)
                    }
                }
                score
            }
            return@withContext rankedList
        }

        return@withContext rawCandidates
    }

    /**
     * Calculates real-time dynamic button text labels based on the current sequence buffer and sentence context.
     * When sequence is empty, uses context predictions to preview next words on matching keys.
     * When sequence is non-empty, calculates next phonetically logical Bengali characters/Kars/Yuktakhor
     * for each button key ('1'..'9') so the user visually sees what each key will produce.
     */
    fun getDynamicKeyLabels(
        seq: String,
        isBanglaMode: Boolean,
        contextSentence: String = ""
    ): Map<Char, String> {
        val result = mutableMapOf<Char, String>()
        val digits = listOf('1', '2', '3', '4', '5', '6', '7', '8', '9')

        if (seq.isEmpty()) {
            val nextWordPredictions = getNextWordPredictions(contextSentence, isBanglaMode)
            for (d in digits) {
                if (isBanglaMode) {
                    val predictedForDigit = nextWordPredictions.filter { word ->
                        word.isNotEmpty() && charToDigit(word.first(), isBangla = true) == d
                    }
                    if (predictedForDigit.isNotEmpty()) {
                        result[d] = predictedForDigit.take(2).joinToString(" ")
                    } else {
                        val defaultText = when (d) {
                            '1' -> "অআই"
                            '2' -> "কখগ"
                            '3' -> "চছজ"
                            '4' -> "টঠড"
                            '5' -> "তথদ"
                            '6' -> "পফব"
                            '7' -> "যরল"
                            '8' -> "ষসহ"
                            '9' -> "ািে"
                            else -> d.toString()
                        }
                        result[d] = defaultText
                    }
                } else {
                    val predictedForDigit = nextWordPredictions.filter { word ->
                        word.isNotEmpty() && charToDigit(word.first(), isBangla = false) == d
                    }
                    if (predictedForDigit.isNotEmpty()) {
                        result[d] = predictedForDigit.take(2).joinToString(" ")
                    } else {
                        val defaultText = when (d) {
                            '1' -> ".,?!"
                            '2' -> "ABC"
                            '3' -> "DEF"
                            '4' -> "GHI"
                            '5' -> "JKL"
                            '6' -> "MNO"
                            '7' -> "PQRS"
                            '8' -> "TUV"
                            '9' -> "WXYZ"
                            else -> d.toString()
                        }
                        result[d] = defaultText
                    }
                }
            }
            return result
        }

        if (!isBanglaMode) {
            for (d in digits) {
                val defaultText = when (d) {
                    '1' -> ".,?!"
                    '2' -> "ABC"
                    '3' -> "DEF"
                    '4' -> "GHI"
                    '5' -> "JKL"
                    '6' -> "MNO"
                    '7' -> "PQRS"
                    '8' -> "TUV"
                    '9' -> "WXYZ"
                    else -> d.toString()
                }
                result[d] = defaultText
            }
            return result
        }

        for (d in digits) {
            val nextSeq = seq + d
            val nextPhonetics = generatePhoneticCombinations(nextSeq, isBangla = true, maxCount = 6)

            val previews = mutableListOf<String>()
            for (ph in nextPhonetics) {
                if (ph.isNotEmpty() && !previews.contains(ph)) {
                    previews.add(ph)
                }
                if (previews.size >= 3) break
            }

            if (previews.isNotEmpty()) {
                result[d] = previews.take(3).joinToString(" ")
            } else {
                val defaultChars = getCharMappings(d, isBangla = true)
                    .filter { !it.first().isDigit() && it != " " }
                    .take(3)
                    .joinToString("")
                result[d] = if (defaultChars.isNotEmpty()) defaultChars else d.toString()
            }
        }

        return result
    }
}
