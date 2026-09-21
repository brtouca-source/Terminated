package com.toucabr.editor.media.export

import com.toucabr.editor.model.*
import org.junit.Assert.*
import org.junit.Test

class ExportTimelineTest {
    @Test fun organicTransitionIsCenteredOnCut() {
        val a = visual("a", 0.0, 2.0)
        val b = visual("b", 2.0, 2.0).copy(transition = TransitionSpec("organic_fusion", duration = .4))
        val project = ToucaProject(clips = listOf(a, b))
        val atCut = requireNotNull(ExportTimeline.transitionAt(project, 2.0))
        assertEquals(.5, atCut.progress, 1e-9)
        assertEquals(1.0, atCut.amount, 1e-9)
        assertNotNull(ExportTimeline.transitionAt(project, 1.81))
        assertNull(ExportTimeline.transitionAt(project, 1.79))
        assertNotNull(ExportTimeline.transitionAt(project, 2.19))
        assertNull(ExportTimeline.transitionAt(project, 2.20))
    }

    @Test fun transitionRequiresTouchingMainClips() {
        val a = visual("a", 0.0, 2.0)
        val b = visual("b", 2.1, 2.0).copy(transition = TransitionSpec("organic_fusion", .4))
        assertNull(ExportTimeline.transitionAt(ToucaProject(clips = listOf(a, b)), 2.1))
    }

    @Test fun mainTransitionFreezesHeadAndTailOutsideClipBounds() {
        val a = video("a", 0.0, 2.0).copy(offset = 3.0)
        val b = video("b", 2.0, 2.0).copy(offset = 6.0, transition = TransitionSpec("organic_fusion", .4))
        val p = ToucaProject(clips = listOf(a, b))
        val before = ExportTimeline.frame(p, 1.9).visuals.associateBy { it.clip.id }
        assertEquals(ExportTimeline.EdgeFreeze.HEAD, before.getValue("b").edgeFreeze)
        assertEquals(6.0, ExportTimeline.sourceTimeFor(before.getValue("b")), 1e-9)
        val after = ExportTimeline.frame(p, 2.1).visuals.associateBy { it.clip.id }
        assertEquals(ExportTimeline.EdgeFreeze.TAIL, after.getValue("a").edgeFreeze)
        assertEquals(4.998, ExportTimeline.sourceTimeFor(after.getValue("a")), 1e-9)
    }

    @Test fun noTransitionOutsideWindow() {
        val a = visual("a", 0.0, 2.0)
        val b = visual("b", 2.0, 2.0).copy(transition = TransitionSpec("organic_fusion", .4))
        val p = ToucaProject(clips = listOf(a, b))
        assertNull(ExportTimeline.frame(p, 1.0).transition)
        assertNull(ExportTimeline.frame(p, 3.0).transition)
    }

    private fun video(id: String, start: Double, duration: Double) = Clip(
        id = id,
        type = ClipType.video,
        name = id,
        track = "main",
        start = start,
        duration = duration,
        keys = listOf(Keyframe(static = true)),
    )

    private fun visual(id: String, start: Double, duration: Double) = Clip(
        id = id,
        type = ClipType.image,
        name = id,
        track = "main",
        start = start,
        duration = duration,
        keys = listOf(Keyframe(static = true)),
    )
}
