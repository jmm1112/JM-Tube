package com.jmtube.app.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.PathParser
import coil3.compose.AsyncImage
import com.jmtube.app.util.AppIcons

// Authentic YouTube Red Pill Path
@Suppress("ktlint:standard:max-line-length")
private const val YOUTUBE_LOGO_BG_PATH = "M21.58 7.16C21.33 6.22 20.59 5.48 19.65 5.23C17.96 4.77 12 4.77 12 4.77C12 4.77 6.04 4.77 4.35 5.23C3.41 5.48 2.67 6.22 2.42 7.16C1.96 8.85 1.96 12.38 1.96 12.38C1.96 12.38 1.96 15.91 2.42 17.6C2.67 18.54 3.41 19.28 4.35 19.53C6.04 19.99 12 19.99 12 19.99C12 19.99 17.96 19.99 19.65 19.53C20.59 19.28 21.33 18.54 21.58 17.6C22.04 15.91 22.04 12.38 22.04 12.38C22.04 12.38 22.04 8.85 21.58 7.16Z"
// Authentic YouTube Play Button Triangle
private const val YOUTUBE_PLAY_TRIANGLE_PATH = "M10 8.5L16 12L10 15.5Z"

// Incognito glyph for deep flow mode
@Suppress("ktlint:standard:max-line-length")
private const val FLOW_INCOGNITO_GLYPH_PATH = "M17.06 13C15.2 13 13.64 14.33 13.24 16.1C12.29 15.69 11.42 15.8 10.76 16.09C10.35 14.31 8.79 13 6.94 13C4.77 13 3 14.79 3 17C3 19.21 4.77 21 6.94 21C9 21 10.68 19.38 10.84 17.32C11.18 17.08 12.07 16.63 13.16 17.34C13.34 19.39 15 21 17.06 21C19.23 21 21 19.21 21 17C21 14.79 19.23 13 17.06 13M6.94 19.86C5.38 19.86 4.13 18.58 4.13 17S5.39 14.14 6.94 14.14C8.5 14.14 9.75 15.42 9.75 17S8.5 19.86 6.94 19.86M17.06 19.86C15.5 19.86 14.25 18.58 14.25 17S15.5 14.14 17.06 14.14C18.62 14.14 19.88 15.42 19.88 17S18.61 19.86 17.06 19.86M22 10.5H2V12H22V10.5M15.53 2.63C15.31 2.14 14.75 1.88 14.22 2.05L12 2.79L9.77 2.05L9.72 2.04C9.19 1.89 8.63 2.17 8.43 2.68L6 9H18L15.56 2.68L15.53 2.63Z"

// Authentic YouTube Music Red Disc and Concentric Ring
private const val YTM_BG_PATH = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z"
private const val YTM_RING_PATH = "M12 6.5c-3.03 0-5.5 2.47-5.5 5.5s2.47 5.5 5.5 5.5s5.5,-2.47 5.5,-5.5S15.03 6.5 12 6.5z M12 15.8c-2.1 0-3.8,-1.7-3.8,-3.8s1.7,-3.8,3.8,-3.8s3.8,1.7,3.8,3.8S14.1 15.8,12 15.8z"
private const val YTM_PLAY_PATH = "M10.2 8.5L15.2 12L10.2 15.5Z"

private val YouTubeRed = Color(0xFFFF0000)

/** The Home logo and the wordmark beside it: a tap does what tapping the Home tab again does, a long-press switches Deep Flow. */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.homeLogoClicks(
    clickLabel: String,
    longClickLabel: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier = combinedClickable(onClickLabel = clickLabel, onLongClickLabel = longClickLabel, onClick = onClick, onLongClick = onLongClick)

/**
 * Smart header logo icon for JM Tube & JM Music.
 * Detects the currently selected launcher icon [iconSuffix] and renders the corresponding icon image.
 * Displays default YouTube or YouTube Music logos if default icon is active or [iconSuffix] is null/default.
 */
@Composable
fun SmartAppHeaderLogoIcon(
    iconSuffix: String? = null,
    isMusic: Boolean = false,
    isDeepFlowActive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val suffix = iconSuffix ?: AppIcons.DEFAULT_SUFFIX
    if (suffix == AppIcons.DARK_SUFFIX) {
        if (isMusic) {
            YouTubeMusicLogoIcon(modifier = modifier)
        } else {
            YouTubeLogoIcon(isDeepFlowActive = isDeepFlowActive, modifier = modifier)
        }
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            AsyncImage(
                model = AppIcons.splashIconRes(suffix),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Authentic original YouTube play-button logo icon.
 */
@Composable
fun YouTubeLogoIcon(
    isDeepFlowActive: Boolean = false,
    iconSuffix: String? = null,
    modifier: Modifier = Modifier,
) {
    val suffix = iconSuffix ?: AppIcons.DEFAULT_SUFFIX
    if (suffix != AppIcons.DARK_SUFFIX) {
        SmartAppHeaderLogoIcon(
            iconSuffix = suffix,
            isMusic = false,
            isDeepFlowActive = isDeepFlowActive,
            modifier = modifier,
        )
        return
    }

    val bgPath = remember { PathParser().parsePathString(YOUTUBE_LOGO_BG_PATH).toPath() }
    val playPath = remember { PathParser().parsePathString(YOUTUBE_PLAY_TRIANGLE_PATH).toPath() }
    val incognitoPath = remember { PathParser().parsePathString(FLOW_INCOGNITO_GLYPH_PATH).toPath() }

    val glyphAlpha by animateFloatAsState(
        targetValue = if (isDeepFlowActive) 0f else 1f,
        animationSpec = tween(durationMillis = 250),
        label = "youtubeGlyphAlpha",
    )
    val incognitoAlpha by animateFloatAsState(
        targetValue = if (isDeepFlowActive) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "youtubeIncognitoAlpha",
    )

    Canvas(modifier = modifier) {
        val scale = minOf(size.width / 24f, size.height / 24f)
        val dx = (size.width - 24f * scale) / 2f
        val dy = (size.height - 24f * scale) / 2f
        drawContext.canvas.save()
        drawContext.canvas.translate(dx, dy)
        drawContext.canvas.scale(scale, scale)

        // Authentic YouTube red pill background
        drawPath(path = bgPath, color = YouTubeRed)

        // Authentic white play triangle
        if (glyphAlpha > 0f) {
            drawPath(path = playPath, color = Color.White.copy(alpha = glyphAlpha))
        }

        // Incognito overlay if deep mode is enabled
        if (incognitoAlpha > 0f) {
            val incognitoScale = 0.65f
            val incognitoOffset = 12f * (1f - incognitoScale)
            drawContext.canvas.save()
            drawContext.canvas.translate(incognitoOffset, incognitoOffset)
            drawContext.canvas.scale(incognitoScale, incognitoScale)
            drawPath(path = incognitoPath, color = Color.White.copy(alpha = incognitoAlpha))
            drawContext.canvas.restore()
        }

        drawContext.canvas.restore()
    }
}

/**
 * Authentic original YouTube Music logo icon.
 */
@Composable
fun YouTubeMusicLogoIcon(
    iconSuffix: String? = null,
    modifier: Modifier = Modifier,
) {
    val suffix = iconSuffix ?: AppIcons.DEFAULT_SUFFIX
    if (suffix != AppIcons.DARK_SUFFIX) {
        SmartAppHeaderLogoIcon(
            iconSuffix = suffix,
            isMusic = true,
            modifier = modifier,
        )
        return
    }

    val bgPath = remember { PathParser().parsePathString(YTM_BG_PATH).toPath() }
    val ringPath =
        remember {
            PathParser().parsePathString(YTM_RING_PATH).toPath().apply {
                fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
            }
        }
    val playPath = remember { PathParser().parsePathString(YTM_PLAY_PATH).toPath() }

    Canvas(modifier = modifier) {
        val scale = minOf(size.width / 24f, size.height / 24f)
        val dx = (size.width - 24f * scale) / 2f
        val dy = (size.height - 24f * scale) / 2f
        drawContext.canvas.save()
        drawContext.canvas.translate(dx, dy)
        drawContext.canvas.scale(scale, scale)

        // YouTube Music Red disc
        drawPath(path = bgPath, color = YouTubeRed)
        // Concentric vinyl ring
        drawPath(path = ringPath, color = Color.White)
        // Center white play triangle
        drawPath(path = playPath, color = Color.White)

        drawContext.canvas.restore()
    }
}

@Composable
internal fun FlowHeaderLogoIcon(
    isDeepFlowActive: Boolean,
    iconSuffix: String? = null,
    modifier: Modifier = Modifier,
) {
    YouTubeLogoIcon(
        isDeepFlowActive = isDeepFlowActive,
        iconSuffix = iconSuffix,
        modifier = modifier,
    )
}
