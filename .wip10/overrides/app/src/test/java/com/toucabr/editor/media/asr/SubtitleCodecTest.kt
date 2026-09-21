package com.toucabr.editor.media.asr

import com.toucabr.editor.model.Clip
import com.toucabr.editor.model.ClipType
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class SubtitleCodecTest {
    @Test fun parsesSrtAndVttAndStripsTags() {
        val srt = """1
00:00:01,000 --> 00:00:02,250
<b>Olá</b> mundo

2
00:02.500 --> 00:03.000
Fim
"""
        val cues = SubtitleCodec.parse(srt)
        assertEquals(2, cues.size)
        assertEquals("Olá mundo", cues[0].text)
        assertEquals(1.0, cues[0].start, 1e-9)
        assertEquals(2.25, cues[0].end, 1e-9)
    }

    @Test fun exportsStableSrtAndVtt() {
        val clips = listOf(
            Clip("b", ClipType.subtitle, "B", track="subtitle", start=2.5, duration=.5, text="Fim"),
            Clip("a", ClipType.subtitle, "A", track="subtitle", start=1.0, duration=1.25, text="Olá"),
        )
        val srt = SubtitleCodec.exportSrt(clips)
        assertTrue(srt.startsWith("1\n00:00:01,000 --> 00:00:02,250\nOlá"))
        val vtt = SubtitleCodec.exportVtt(clips)
        assertTrue(vtt.startsWith("WEBVTT\n\n00:00:01.000 --> 00:00:02.250\nOlá"))
    }
}
