package com.yuyagioh.tcgreader.core.util

import kotlin.math.min

object TextMatcher {

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val previous = IntArray(b.length + 1) { it }
        val current = IntArray(b.length + 1)
        for (i in a.indices) {
            current[0] = i + 1
            for (j in b.indices) {
                val substitutionCost = if (a[i] == b[j]) 0 else 1
                current[j + 1] = min(
                    min(current[j] + 1, previous[j + 1] + 1),
                    previous[j] + substitutionCost,
                )
            }
            System.arraycopy(current, 0, previous, 0, current.size)
        }
        return previous[b.length]
    }

    fun normalize(text: String): String = text
        .lowercase()
        .filter { it.isLetterOrDigit() || it.isWhitespace() }
        .replace(Regex("\\s+"), " ")
        .trim()

    fun isSimilar(a: String, b: String, maxDistance: Int = 2): Boolean =
        levenshtein(normalize(a), normalize(b)) <= maxDistance

    fun bestMatch(input: String, candidates: List<String>, maxDistance: Int = 2): Pair<String, Int>? =
        candidates
            .map { it to levenshtein(normalize(input), normalize(it)) }
            .filter { it.second <= maxDistance }
            .minByOrNull { it.second }
}
