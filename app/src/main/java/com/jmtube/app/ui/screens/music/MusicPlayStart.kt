package com.jmtube.app.ui.screens.music

import android.net.Uri
import com.jmtube.app.data.local.PlayerPreferences
import com.jmtube.app.data.local.ViewHistory
import com.jmtube.app.data.music.DownloadManager
import com.jmtube.app.data.music.model.MusicTrack
import com.jmtube.app.player.MusicResumePolicy
import com.jmtube.app.utils.PerformanceDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/** What a tapped play reads from storage before it starts, and the history row it leaves behind. */
internal class MusicPlayStart(
    private val downloadManager: DownloadManager,
    private val viewHistory: ViewHistory,
    private val playerPreferences: PlayerPreferences,
) {
    /** Offline copies of [queue], so they play from the device instead of the network. */
    suspend fun downloadedUris(queue: List<MusicTrack>): Map<String, Uri> =
        withContext(PerformanceDispatcher.diskIO) {
            queue
                .mapNotNull { track ->
                    val path = downloadManager.getDownloadedTrackPath(track.videoId) ?: return@mapNotNull null
                    val uri = if (path.startsWith("content://")) Uri.parse(path) else Uri.fromFile(File(path))
                    track.videoId to uri
                }.toMap()
        }

    /** Where [track] should start: its saved place when it is long and resuming is on, else 0. */
    suspend fun resumePosition(track: MusicTrack): Long {
        if (!playerPreferences.musicResumeLongTracks.first()) return 0L
        return withContext(PerformanceDispatcher.diskIO) {
            val durationMs = track.duration * 1000L
            MusicResumePolicy.startPosition(
                savedPosition = viewHistory.getSavedPosition(track.videoId),
                durationMs = durationMs.takeIf { it > 0L } ?: viewHistory.getSavedDuration(track.videoId),
            )
        }
    }

    suspend fun recordStart(track: MusicTrack) =
        viewHistory.touchMusicEntry(
            videoId = track.videoId,
            duration = track.duration.toLong() * 1000,
            title = track.title,
            thumbnailUrl = track.thumbnailUrl,
            channelName = track.artist,
            channelId = track.channelId,
        )
}
