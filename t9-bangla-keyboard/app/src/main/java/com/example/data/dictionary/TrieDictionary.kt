package com.example.data.dictionary

data class TrieNode(
    val children: MutableMap<Char, TrieNode> = mutableMapOf(),
    var isWord: Boolean = false,
    var word: String? = null,
    var frequency: Int = 0,
    var baseSequence: String? = null,
    var fullSequence: String? = null
)

data class CandidateResult(
    val word: String,
    val score: Int,
    val isExactMatch: Boolean
)

class BengaliTrieDictionary {

    val root = TrieNode()
    private val baseSeqIndex = mutableMapOf<String, MutableList<Pair<String, Int>>>()
    private val fullSeqIndex = mutableMapOf<String, MutableList<Pair<String, Int>>>()
    private val bigramMap = mutableMapOf<String, MutableMap<String, Int>>()

    init {
        populateBuiltInDictionary()
        populateBigrams()
    }

    fun insert(word: String, frequency: Int = 1) {
        val baseSeq = BanglaDictionary.wordToBaseSequence(word)
        val fullSeq = BanglaDictionary.wordToFullSequence(word)

        // 1. Insert into Trie Character Graph
        var current = root
        for (ch in word) {
            current = current.children.getOrPut(ch) { TrieNode() }
        }
        current.isWord = true
        current.word = word
        current.frequency = maxOf(current.frequency, frequency)
        current.baseSequence = baseSeq
        current.fullSequence = fullSeq

        // 2. Index in Base Sequence Map
        if (baseSeq.isNotEmpty()) {
            val list = baseSeqIndex.getOrPut(baseSeq) { mutableListOf() }
            if (list.none { it.first == word }) {
                list.add(word to frequency)
            }
        }

        // 3. Index in Full Sequence Map
        if (fullSeq.isNotEmpty() && fullSeq != baseSeq) {
            val list = fullSeqIndex.getOrPut(fullSeq) { mutableListOf() }
            if (list.none { it.first == word }) {
                list.add(word to frequency)
            }
        }
    }

    fun searchBySequence(
        sequence: String,
        prevWord: String? = null,
        userCustomWords: List<String> = emptyList(),
        maxResults: Int = 8
    ): List<CandidateResult> {
        if (sequence.isEmpty()) return emptyList()

        val candidateMap = mutableMapOf<String, CandidateResult>()

        // 1. Search in Custom User Words
        for (uWord in userCustomWords) {
            val uBaseSeq = BanglaDictionary.wordToBaseSequence(uWord)
            val uFullSeq = BanglaDictionary.wordToFullSequence(uWord)
            if (uBaseSeq == sequence || uFullSeq == sequence || uBaseSeq.startsWith(sequence)) {
                val isExact = (uBaseSeq == sequence || uFullSeq == sequence)
                val baseScore = if (isExact) 1200 else 600
                val contextBoost = getBigramScore(prevWord, uWord) * 50
                candidateMap[uWord] = CandidateResult(uWord, baseScore + contextBoost, isExact)
            }
        }

        // 2. Search in Base Sequence Index (Matches user tapping base keys without Kars)
        val baseMatches = baseSeqIndex[sequence]
        if (baseMatches != null) {
            for ((word, freq) in baseMatches) {
                if (!candidateMap.containsKey(word)) {
                    val contextScore = getBigramScore(prevWord, word) * 40
                    candidateMap[word] = CandidateResult(word, 500 + freq + contextScore, true)
                }
            }
        }

        // 3. Search Prefix Matches in Base Sequence Index
        for ((bSeq, wordList) in baseSeqIndex) {
            if (bSeq.startsWith(sequence) && bSeq != sequence) {
                for ((word, freq) in wordList) {
                    if (!candidateMap.containsKey(word)) {
                        val contextScore = getBigramScore(prevWord, word) * 20
                        candidateMap[word] = CandidateResult(word, 200 + freq + contextScore, false)
                    }
                }
            }
        }

        // 4. Search in Full Sequence Index (Matches explicit Kar key '1')
        val fullMatches = fullSeqIndex[sequence]
        if (fullMatches != null) {
            for ((word, freq) in fullMatches) {
                if (!candidateMap.containsKey(word)) {
                    val contextScore = getBigramScore(prevWord, word) * 40
                    candidateMap[word] = CandidateResult(word, 450 + freq + contextScore, true)
                }
            }
        }

        // 5. Trie Deep Search Fallback
        dfsSequenceMatch(root, sequence, 0, prevWord, candidateMap)

        return candidateMap.values
            .sortedByDescending { it.score }
            .take(maxResults)
    }

    private fun dfsSequenceMatch(
        node: TrieNode,
        targetSeq: String,
        seqIndex: Int,
        prevWord: String?,
        candidateMap: MutableMap<String, CandidateResult>
    ) {
        if (node.isWord && node.word != null) {
            val word = node.word!!
            val bSeq = node.baseSequence ?: BanglaDictionary.wordToBaseSequence(word)
            val fSeq = node.fullSequence ?: BanglaDictionary.wordToFullSequence(word)

            if (bSeq == targetSeq || fSeq == targetSeq) {
                val contextBoost = getBigramScore(prevWord, word) * 50
                val totalScore = 400 + node.frequency + contextBoost
                candidateMap[word] = CandidateResult(word, totalScore, isExactMatch = true)
            } else if (bSeq.startsWith(targetSeq) || fSeq.startsWith(targetSeq)) {
                val contextBoost = getBigramScore(prevWord, word) * 30
                val totalScore = 150 + node.frequency + contextBoost
                candidateMap[word] = CandidateResult(word, totalScore, isExactMatch = false)
            }
        }

        if (seqIndex < targetSeq.length) {
            val expectedKeyChar = targetSeq[seqIndex]
            val allowedChars = BanglaDictionary.keyCharMap[expectedKeyChar] ?: emptyList()

            for ((charVal, childNode) in node.children) {
                val isMatch = allowedChars.contains(charVal)
                val isKarOrJukto = BanglaDictionary.isKar(charVal) || charVal == '্'
                if (isMatch || isKarOrJukto) {
                    val nextSeqIdx = if (isMatch) seqIndex + 1 else seqIndex
                    dfsSequenceMatch(childNode, targetSeq, nextSeqIdx, prevWord, candidateMap)
                }
            }
        } else {
            // Traversal at end of sequence
            for ((_, childNode) in node.children) {
                dfsSequenceMatch(childNode, targetSeq, seqIndex, prevWord, candidateMap)
            }
        }
    }

    fun getBigramScore(prevWord: String?, currentWord: String): Int {
        if (prevWord.isNullOrBlank()) return 0
        val wordBigrams = bigramMap[prevWord.trim()] ?: return 0
        return wordBigrams[currentWord.trim()] ?: 0
    }

    fun addBigram(prevWord: String, currentWord: String, weight: Int = 1) {
        val p = prevWord.trim()
        val c = currentWord.trim()
        if (p.isNotEmpty() && c.isNotEmpty()) {
            val map = bigramMap.getOrPut(p) { mutableMapOf() }
            map[c] = (map[c] ?: 0) + weight
        }
    }

    private fun populateBigrams() {
        val sampleBigrams = listOf(
            "আমি" to listOf("আছি" to 90, "যাব" to 85, "বলব" to 80, "তোমাকে" to 95, "ভালোবাসি" to 92, "কাজ" to 75, "বাংলা" to 80),
            "আমার" to listOf("সোনার" to 100, "দেশ" to 95, "নাম" to 90, "বাংলাদেশ" to 95, "মা" to 85, "বন্ধু" to 88, "ভাষা" to 90),
            "আমাদের" to listOf("দেশ" to 95, "বাংলাদেশ" to 90, "স্কুল" to 80, "ভাষা" to 85, "সংস্কৃতি" to 80),
            "তুমি" to listOf("কেমন" to 100, "আছো" to 98, "কোথায়" to 90, "কী" to 85, "যাবে" to 80),
            "তোমার" to listOf("নাম" to 98, "বাড়ি" to 85, "কথা" to 80, "ফোন" to 75),
            "শুভ" to listOf("সকাল" to 100, "রাত্রি" to 95, "জন্মদিন" to 98, "কামনা" to 90, "ঈদ" to 92),
            "কেমন" to listOf("আছো" to 100, "আছেন" to 95, "আছিস" to 88),
            "ধন্যবাদ" to listOf("তোমাকে" to 95, "আপনাকে" to 90, "ভাই" to 85),
            "বাংলাদেশ" to listOf("আমার" to 90, "আমাদের" to 85, "দেশ" to 95, "জিন্দাবাদ" to 80),
            "বাংলা" to listOf("ভাষা" to 100, "সংবাদ" to 85, "কীবোর্ড" to 90),
            "অনেক" to listOf("ধন্যবাদ" to 98, "সুন্দর" to 92, "ভাল" to 90, "ভালোবাসা" to 88)
        )

        for ((prev, nextList) in sampleBigrams) {
            for ((next, weight) in nextList) {
                addBigram(prev, next, weight)
            }
        }
    }

    private fun populateBuiltInDictionary() {
        var baseFreq = 100
        for (word in BanglaDictionary.builtInWords) {
            insert(word, baseFreq)
            baseFreq = maxOf(20, baseFreq - 1)
        }
    }
}

// Alias for backwards compatibility
typealias TrieDictionary = BengaliTrieDictionary
