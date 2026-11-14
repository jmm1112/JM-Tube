package com.jmtube.app.ui.components.musicplayer.mini

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jmtube.app.data.lyrics.LyricsUtils
import com.jmtube.app.ui.components.musicplayer.lyrics.SyncedLines
import com.jmtube.app.ui.screens.music.MusicPlayerViewModel
import com.jmtube.app.ui.screens.music.sharedMusicPlayerViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The synced lyrics the music player already holds for [videoId], for the mini player's lyric line.
 * Nothing is looked up here: the player loads them per song, and this only reads what it has.
 */
@Composable
internal fun rememberMiniPlayerLyrics(
    videoId: String,
    viewModel: MusicPlayerViewModel = sharedMusicPlayerViewModel(),
): SyncedLines? {
    val lyrics =
        remember(viewModel, videoId) {
            viewModel.uiState
                .map { state ->
                    val synced = state.syncedLyrics.takeIf { state.currentTrack?.videoId == videoId }.orEmpty()
                    synced to state.lyricsSyncOffsetMs
                }.distinctUntilChanged()
                .map { (synced, offsetMs) ->
                    // Blank lines stay: they are the breaks the line clears for.
                    LyricsUtils
                        .filterCreditLines(synced)
                        .filterNot { it.isBackground }
                        .sortedBy { it.time }
                        .takeIf { lines -> lines.any { it.text.isNotBlank() } }
                        ?.let { SyncedLines(it, offsetMs) }
                }
        }
    val value by lyrics.collectAsStateWithLifecycle(initialValue = null)
    return value
}
