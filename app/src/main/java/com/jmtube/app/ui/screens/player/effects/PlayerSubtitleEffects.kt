package com.jmtube.app.ui.screens.player.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.jmtube.app.ui.components.videoplayer.subtitle.SubtitleStyle
import com.jmtube.app.ui.screens.player.state.PlayerScreenState

/**
 * Mirrors the persisted subtitle style onto the screen state. Which track plays, including turning
 * captions on automatically, is the player's own decision.
 */
@Composable
internal fun PlayerSubtitleEffects(
    screenState: PlayerScreenState,
    savedSubtitleStyle: SubtitleStyle,
) {
    LaunchedEffect(savedSubtitleStyle) {
        if (screenState.subtitleStyle != savedSubtitleStyle) {
            screenState.subtitleStyle = savedSubtitleStyle
        }
    }
}
