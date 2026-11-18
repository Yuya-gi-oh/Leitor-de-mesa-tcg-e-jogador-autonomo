package com.yuyagioh.tcgreader.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextMatcherTest {

    @Test
    fun levenshteinComputesClassicDistances() {
        assertEquals(0, TextMatcher.levenshtein("kitten", "kitten"))
        assertEquals(3, TextMatcher.levenshtein("kitten", "sitting"))
        assertEquals(1, TextMatcher.levenshtein("flaw", "lawn"))
    }

    @Test
    fun normalizeIgnoresCasePunctuationAndExtraWhitespace() {
        assertEquals("bolt", TextMatcher.normalize("  BOLT!!!  "))
        assertTrue(TextMatcher.isSimilar("Lightning Bolt", "lightnig bolt"))
        assertFalse(TextMatcher.isSimilar("Lightning Bolt", "Doom Blade"))
    }

    @Test
    fun bestMatchPicksTheClosestCandidate() {
        val best = TextMatcher.bestMatch(
            "grizzly beares",
            listOf("Grizzly Bears", "Hill Giant", "Shock"),
        )
        assertEquals("Grizzly Bears", best?.first)
    }
}
