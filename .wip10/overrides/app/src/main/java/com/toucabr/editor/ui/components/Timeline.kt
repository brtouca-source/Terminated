package com.toucabr.editor.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toucabr.editor.interaction.ClipGestureMode
import com.toucabr.editor.interaction.ClipGestureResolver
import com.toucabr.editor.model.*
import com.toucabr.editor.media.audio.WaveformMath
import kotlin.math.max

private val trackOrder = Tracks.ALL

@Composable
fun Timeline(
    project: ToucaProject,
    playhead: Double,
    pps: Double,
    selectedClipId: String?,
    playing: Boolean,
    waveforms: Map<String, FloatArray> = emptyMap(),
    onSelect: (String?) -> Unit,
    onSeek: (Double) -> Unit,
    onMoveClip: (String, Double, String?) -> Unit,
    onTrimLeft: (String, Double) -> Unit,
    onTrimRight: (String, Double) -> Unit,
    onBeginGesture: () -> Unit,
    onCommitGesture: () -> Unit,
    onCancelGesture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val duration = max(12.0, ProjectMath.duration(project) + 4.0)
    val widthDp = (duration * pps).dp
    val assetMap = remember(project.assets) { project.assets.associateBy { it.id } }
    val visibleTracks = remember(project.clips) {
        val used = project.clips.map { it.track }.toSet()
        trackOrder.filter { it in used || it in setOf("main", "overlay", "subtitle", "voice", "effects") }
    }

    Row(modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.width(76.dp)) {
            Spacer(Modifier.height(28.dp))
            visibleTracks.forEach { track ->
                Box(Modifier.height(52.dp).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    Text(track, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp), maxLines = 1)
                }
            }
        }
        Box(Modifier.weight(1f).horizontalScroll(scroll)) {
            Column(Modifier.width(widthDp)) {
                Ruler(duration, pps, onSeek)
                visibleTracks.forEach { track ->
                    TrackRow(
                        track = track,
                        clips = project.clips.filter { it.track == track },
                        pps = pps,
                        selectedClipId = selectedClipId,
                        fps = project.fps,
                        assets = assetMap,
                        waveforms = waveforms,
                        onSelect = onSelect,
                        onMoveClip = onMoveClip,
                        onTrimLeft = onTrimLeft,
                        onTrimRight = onTrimRight,
                        onBeginGesture = onBeginGesture,
                        onCommitGesture = onCommitGesture,
                        onCancelGesture = onCancelGesture,
                    )
                }
            }
            Box(
                Modifier
                    .offset(x = (playhead * pps).dp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.error)
            )
        }
    }
}

@Composable
private fun Ruler(duration: Double, pps: Double, onSeek: (Double) -> Unit) {
    val density = LocalDensity.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .pointerInput(pps) {
                detectDragGestures(
                    onDragStart = { p -> onSeek((p.x / density.density / pps).toDouble()) },
                    onDrag = { change, _ ->
                        change.consume()
                        onSeek((change.position.x / density.density / pps).toDouble())
                    }
                )
            }
    ) {
        val seconds = duration.toInt()
        for (s in 0..seconds) {
            if (s % (if (pps < 25) 5 else 1) != 0) continue
            Text(
                "$s",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.offset(x = (s * pps).dp).padding(start = 2.dp, top = 5.dp)
            )
        }
    }
}

@Composable
private fun TrackRow(
    track: String,
    clips: List<Clip>,
    pps: Double,
    selectedClipId: String?,
    fps: Int,
    assets: Map<String, Asset>,
    waveforms: Map<String, FloatArray>,
    onSelect: (String?) -> Unit,
    onMoveClip: (String, Double, String?) -> Unit,
    onTrimLeft: (String, Double) -> Unit,
    onTrimRight: (String, Double) -> Unit,
    onBeginGesture: () -> Unit,
    onCommitGesture: () -> Unit,
    onCancelGesture: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        clips.forEach { clip ->
            ClipBar(
                clip = clip,
                pps = pps,
                selected = clip.id == selectedClipId,
                fps = fps,
                waveform = clip.asset?.let(waveforms::get),
                assetDuration = clip.asset?.let(assets::get)?.duration,
                onSelect = onSelect,
                onMove = { seconds -> onMoveClip(clip.id, seconds, track) },
                onTrimLeft = { onTrimLeft(clip.id, it) },
                onTrimRight = { onTrimRight(clip.id, it) },
                onBeginGesture = onBeginGesture,
                onCommitGesture = onCommitGesture,
                onCancelGesture = onCancelGesture,
            )
        }
    }
}


@Composable
private fun ClipBar(
    clip: Clip,
    pps: Double,
    selected: Boolean,
    fps: Int,
    waveform: FloatArray?,
    assetDuration: Double?,
    onSelect: (String?) -> Unit,
    onMove: (Double) -> Unit,
    onTrimLeft: (Double) -> Unit,
    onTrimRight: (Double) -> Unit,
    onBeginGesture: () -> Unit,
    onCommitGesture: () -> Unit,
    onCancelGesture: () -> Unit,
) {
    val density = LocalDensity.current
    val visualGap = 8.0
    val width = max(10.0, clip.duration * pps - visualGap).dp
    val x = (clip.start * pps + visualGap / 2.0).dp
    var measuredWidthPx by remember(clip.id) { mutableIntStateOf(0) }
    val selectedState = rememberUpdatedState(selected)
    val clipStartState = rememberUpdatedState(clip.start)

    val baseColor = when (clip.type) {
        ClipType.image -> MaterialTheme.colorScheme.primaryContainer
        ClipType.video -> MaterialTheme.colorScheme.tertiaryContainer
        ClipType.audio -> MaterialTheme.colorScheme.secondaryContainer
        ClipType.subtitle -> MaterialTheme.colorScheme.surfaceVariant
        ClipType.text -> MaterialTheme.colorScheme.inversePrimary
        ClipType.effect -> MaterialTheme.colorScheme.errorContainer
    }

    Box(
        Modifier
            .offset(x = x)
            .width(width)
            .height(46.dp)
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(baseColor)
            .border(if (selected) 2.dp else 0.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(7.dp))
            .onSizeChanged { measuredWidthPx = it.width }
            // Um único recognizer possui o ponteiro do começo ao fim. As alças abaixo são apenas visuais.
            // Isso impede MOVE/TRIM de disputarem o mesmo dedo quando os hitboxes de 48dp se sobrepõem.
            .pointerInput(clip.id, pps) {
                val handlePx = with(density) { 48.dp.toPx() }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val selectedNow = selectedState.value
                    val widthPx = measuredWidthPx.toFloat().coerceAtLeast(1f)
                    val mode = ClipGestureResolver.resolve(
                        xPx = down.position.x,
                        clipWidthPx = widthPx,
                        selected = selectedNow,
                        handleHitPx = handlePx,
                    )

                    val pointerId = down.id
                    val touchSlop = viewConfiguration.touchSlop
                    var last = down.position
                    var totalDx = 0f
                    var totalDistanceSquared = 0f
                    var dragging = false
                    var cancelled = false
                    val originalStart = clipStartState.value

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId }
                        if (change == null) {
                            cancelled = true
                            break
                        }
                        if (!change.pressed) break

                        val delta = change.position - last
                        last = change.position
                        totalDx += delta.x
                        totalDistanceSquared += delta.x * delta.x + delta.y * delta.y

                        if (!dragging && totalDistanceSquared >= touchSlop * touchSlop) {
                            dragging = true
                            onSelect(clip.id)
                            onBeginGesture()
                        }
                        if (dragging) {
                            change.consume()
                            val dxSeconds = delta.x / density.density / pps
                            when (mode) {
                                ClipGestureMode.MOVE -> onMove(originalStart + totalDx / density.density / pps)
                                ClipGestureMode.TRIM_LEFT -> onTrimLeft(dxSeconds)
                                ClipGestureMode.TRIM_RIGHT -> onTrimRight(dxSeconds)
                            }
                        }
                    }

                    if (dragging) {
                        if (cancelled) onCancelGesture() else onCommitGesture()
                    } else if (!cancelled) {
                        onSelect(clip.id)
                    }
                }
            }
    ) {
        if (waveform != null && waveform.isNotEmpty() && assetDuration != null && assetDuration > 0.0) {
            val sliced = remember(waveform, clip.offset, clip.duration, assetDuration) {
                WaveformMath.slice(waveform, assetDuration, clip.offset, clip.duration, maxPoints = 256)
            }
            val waveColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .48f)
            Canvas(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 7.dp)) {
                if (sliced.isNotEmpty()) {
                    val step = size.width / sliced.size.coerceAtLeast(1)
                    val cy = size.height / 2f
                    sliced.forEachIndexed { i, peak ->
                        val half = (peak.coerceIn(.04f, 1f) * cy)
                        val x = (i + .5f) * step
                        drawLine(waveColor, start = androidx.compose.ui.geometry.Offset(x, cy - half), end = androidx.compose.ui.geometry.Offset(x, cy + half), strokeWidth = maxOf(1f, step * .45f))
                    }
                }
            }
        }
        Text(
            clip.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 18.dp)
        )
        if (selected) {
            // 5dp visuais; hitbox lógico de 48dp é resolvido pelo recognizer único acima.
            Box(Modifier.align(Alignment.CenterStart).width(5.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
            Box(Modifier.align(Alignment.CenterEnd).width(5.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        }
        clip.keys.filterNot { it.static }.forEach { key ->
            Box(
                Modifier
                    .offset(x = (key.t * pps).dp - 5.dp)
                    .align(Alignment.CenterStart)
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer, RoundedCornerShape(2.dp))
            )
        }
    }
}