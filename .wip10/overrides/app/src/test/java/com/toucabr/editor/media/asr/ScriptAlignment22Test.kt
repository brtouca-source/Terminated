package com.toucabr.editor.media.asr

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class ScriptAlignment22Test {
    @Test
    fun preservesExactScriptSpellingPunctuationAndMonotonicTiming() {
        val input = listOf(
            TranscriptWord("Esse", 0.0, .2),
            TranscriptWord("é", .2, .35),
            TranscriptWord("o", .35, .45),
            TranscriptWord("Tech", .45, .7),
            TranscriptWord("Jacket", .7, 1.0),
        )
        val out = ScriptAlignment22.align("Esse é o Tech Jacket!", input, 1.0)
        assertEquals(listOf("Esse", "é", "o", "Tech", "Jacket!"), out.words.map { it.text })
        assertEquals(5, out.matched)
        assertTrue(out.words.zipWithNext().all { (a, b) -> a.end <= b.start + 1e-9 })
        assertTrue(out.confidence >= 95)
    }

    @Test
    fun mergedAsrWordsAnchorFairyland() {
        val out = ScriptAlignment22.align(
            "Fairyland voltou!",
            listOf(TranscriptWord("Fairy", 0.0, .25), TranscriptWord("Land", .25, .5), TranscriptWord("voltou", .5, .9)),
            1.0,
        )
        assertEquals("Fairyland", out.words.first().text)
        assertTrue(out.words.first().anchor)
        assertTrue(out.words.last().end <= 1.0)
    }

    @Test
    fun handlesLongNarrationInBandedMemory() {
        val n = 3000
        val text = (0 until n).joinToString(" ") { "palavra$it" }
        val chunks = (0 until n).map { TranscriptWord("palavra$it", it * .3, (it + 1) * .3) }
        val out = ScriptAlignment22.align(text, chunks, n * .3)
        assertEquals(n, out.matched)
        assertEquals(n, out.words.size)
    }
}