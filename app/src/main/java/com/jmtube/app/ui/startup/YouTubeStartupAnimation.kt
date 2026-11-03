package com.jmtube.app.ui.startup

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val YouTubeSplashBg = Color(0xFF0F0F0F)
private val YouTubeBrandRed = Color(0xFFFF0000)
private val SeekTrackColor = Color(0x38FFFFFF)
private val SeekBufferColor = Color(0x66FFFFFF)

private val SmoothMorphEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
private val SnapLockupEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

/**
 * Authentic YouTube app opening animation sequence:
 * 1. Starts on a dark background (#0F0F0F) displaying the red YouTube rounded rectangle and white play triangle.
 * 2. Morphs and expands horizontally into an expanding timeline / seek bar with a sweeping red progress indicator & scrubber.
 * 3. Resolves into the classic YouTube logo lockup (Red YouTube pill + white play triangle + condensed bold wordmark).
 * 4. Smoothly fades out into the main app interface.
 */
@Composable
internal fun YouTubeStartupAnimation(
    onFinished: () -> Unit,
) {
    var completed by remember { mutableStateOf(false) }
    if (completed) return

    // Phase 1: Initial icon pop-in (0f -> 1f)
    val introScale = remember { Animatable(0.88f) }
    // Phase 2a: Icon morphs into horizontal seek bar (0f = YouTube Pill, 1f = Timeline Seek Bar)
    val morphToSeekBar = remember { Animatable(0f) }
    // Phase 2b: Red timeline progress sweeps from left to right (0f -> 1f)
    val seekProgress = remember { Animatable(0f) }
    // Phase 3: Timeline resolves back into YouTube Pill and slides left to reveal the classic logo lockup (0f -> 1f)
    val lockupResolve = remember { Animatable(0f) }
    // Phase 4: Entire startup screen fades out into the main app interface (1f -> 0f)
    val overlayAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Stage 1: Pop in the YouTube Red Pill + White Play Triangle on dark background
        introScale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 260, easing = LinearOutSlowInEasing),
        )
        delay(80L)

        // Stage 2: Morph the YouTube Pill into the expanding timeline / seek bar and sweep the progress
        val morphJob =
            launch {
                morphToSeekBar.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 300, easing = SmoothMorphEasing),
                )
            }
        delay(110L)
        seekProgress.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        )
        morphJob.join()

        // Stage 3: Collapse timeline back into the YouTube Pill & resolve into the classic YouTube logo lockup
        coroutineScopeMorphToLockup(
            morphToSeekBar = morphToSeekBar,
            lockupResolve = lockupResolve,
        )

        // Hold the resolved classic logo lockup briefly so it reads crisply
        delay(260L)

        // Stage 4: Smoothly fade out and subtly zoom into the main app interface
        launch {
            exitScale.animateTo(
                targetValue = 1.06f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            )
        }
        overlayAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        )
        completed = true
        onFinished()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = overlayAlpha.value
                    scaleX = introScale.value * exitScale.value
                    scaleY = introScale.value * exitScale.value
                }.background(YouTubeSplashBg)
                .pointerInput(Unit) {
                    // Block stray touches during the opening animation transition
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                        }
                    }
                }.testTag("youtube_startup_animation"),
        contentAlignment = Alignment.Center,
    ) {
        val morph = morphToSeekBar.value
        val seek = seekProgress.value
        val lockup = lockupResolve.value

        // Canvas draws the morphing YouTube Pill <-> Expanding Timeline/Seek Bar
        Canvas(
            modifier =
                Modifier
                    .size(width = 280.dp, height = 88.dp),
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f

            // Dimensions in px
            val startPillW = 78.dp.toPx()
            val startPillH = 54.dp.toPx()
            val startCorner = 15.dp.toPx()

            val lockupPillW = 52.dp.toPx()
            val lockupPillH = 36.dp.toPx()
            val lockupCorner = 10.dp.toPx()

            val seekBarW = 228.dp.toPx()
            val seekBarH = 6.dp.toPx()
            val seekCorner = 3.dp.toPx()

            // When resolving into the lockup, the icon shifts left so the icon + wordmark lockup is centered
            val lockupShiftX = -58.dp.toPx() * lockup

            // Base pill dimensions (interpolating from initial big center pill to lockup pill size)
            val targetPillW = lerp(startPillW, lockupPillW, lockup)
            val targetPillH = lerp(startPillH, lockupPillH, lockup)
            val targetPillCorner = lerp(startCorner, lockupCorner, lockup)

            // Current shape dimensions between Pill (morph = 0f) and Timeline Seek Bar (morph = 1f)
            val currentW = lerp(targetPillW, seekBarW, morph)
            val currentH = lerp(targetPillH, seekBarH, morph)
            val currentCorner = lerp(targetPillCorner, seekCorner, morph)

            val left = centerX + lockupShiftX - currentW / 2f
            val top = centerY - currentH / 2f

            if (morph > 0.02f) {
                // Draw the timeline background track and buffered track during the seek bar phase
                val trackAlpha = (morph * (1f - lockup * 0.5f)).coerceIn(0f, 1f)
                drawRoundRect(
                    color = SeekTrackColor.copy(alpha = SeekTrackColor.alpha * trackAlpha),
                    topLeft = Offset(left, top),
                    size = Size(currentW, currentH),
                    cornerRadius = CornerRadius(currentCorner, currentCorner),
                )

                val bufferProgress = (seek * 1.25f + 0.15f).coerceIn(0f, 1f)
                val bufferW = currentW * lerp(1f, bufferProgress, morph)
                drawRoundRect(
                    color = SeekBufferColor.copy(alpha = SeekBufferColor.alpha * trackAlpha),
                    topLeft = Offset(left, top),
                    size = Size(bufferW, currentH),
                    cornerRadius = CornerRadius(currentCorner, currentCorner),
                )
            }

            // Active Red Pill / Red Timeline Progress Fill
            val fillFraction =
                if (lockup > 0f) {
                    lerp(seek.coerceAtLeast(0.18f), 1f, lockup)
                } else {
                    lerp(1f, seek.coerceAtLeast(0.14f), morph)
                }
            val activeRedW = (currentW * fillFraction).coerceAtLeast(currentH)

            drawRoundRect(
                color = YouTubeBrandRed,
                topLeft = Offset(left, top),
                size = Size(activeRedW, currentH),
                cornerRadius = CornerRadius(currentCorner, currentCorner),
            )

            // Glowing red scrubber thumb at the leading edge of the timeline during the seek bar phase
            val scrubberVisibility = (morph * (1f - lockup)).coerceIn(0f, 1f)
            if (scrubberVisibility > 0.05f) {
                val thumbX = (left + activeRedW).coerceIn(left + currentCorner, left + currentW - currentCorner)
                val thumbRadius = 6.5.dp.toPx() * scrubberVisibility
                val haloRadius = 14.dp.toPx() * scrubberVisibility
                drawCircle(
                    color = YouTubeBrandRed.copy(alpha = 0.28f * scrubberVisibility),
                    radius = haloRadius,
                    center = Offset(thumbX, centerY),
                )
                drawCircle(
                    color = YouTubeBrandRed,
                    radius = thumbRadius,
                    center = Offset(thumbX, centerY),
                )
            }

            // Crisp White Play Triangle inside the YouTube Pill (fades out as pill morphs into seekbar, returns on lockup)
            val triangleAlpha = (1f - morph * 1.35f).coerceIn(0f, 1f)
            if (triangleAlpha > 0.01f) {
                val triCenterX = centerX + lockupShiftX
                val triScale = lerp(1f, lockupPillW / startPillW, lockup) * triangleAlpha
                val halfH = 10.5.dp.toPx() * triScale
                val leftX = triCenterX - 6.5.dp.toPx() * triScale
                val rightX = triCenterX + 9.5.dp.toPx() * triScale

                val trianglePath =
                    Path().apply {
                        moveTo(leftX, centerY - halfH)
                        lineTo(rightX, centerY)
                        lineTo(leftX, centerY + halfH)
                        close()
                    }
                drawPath(
                    path = trianglePath,
                    color = Color.White.copy(alpha = triangleAlpha),
                )
            }
        }

        // Classic YouTube Wordmark Lockup resolving beside the icon in Stage 3
        if (lockup > 0.01f) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .offset {
                            val slidePx = ((1f - lockup) * (-18).dp.toPx()).roundToInt()
                            IntOffset(x = 30.dp.roundToPx() + slidePx, y = 0)
                        }.graphicsLayer {
                            alpha = lockup.coerceIn(0f, 1f)
                        },
            ) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "JM Tube",
                    color = Color.White,
                    fontSize = 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-1.2).sp,
                )
            }
        }
    }
}

private suspend fun coroutineScopeMorphToLockup(
    morphToSeekBar: Animatable<Float, *>,
    lockupResolve: Animatable<Float, *>,
) {
    kotlinx.coroutines.coroutineScope {
        launch {
            morphToSeekBar.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 360, easing = SnapLockupEasing),
            )
        }
        launch {
            lockupResolve.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 380, easing = SnapLockupEasing),
            )
        }
    }
}
