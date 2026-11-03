package com.jmtube.app.ui.components.shared

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jmtube.app.ui.screens.home.YouTubeLogoIcon

/**
 * The app's badge, drawn with the authentic original YouTube logo at its natural 1:1 viewport aspect ratio
 * so it never stretches horizontally or vertically.
 */
@Composable
fun FlowLogo(modifier: Modifier = Modifier) {
    YouTubeLogoIcon(modifier = modifier.aspectRatio(1f))
}
