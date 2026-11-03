package com.jmtube.app.player

object PlaybackStartupPolicy {
    fun shouldDelaySecondaryContent(
        isPlaybackLoading: Boolean,
        currentVideoId: String?,
        requestedVideoId: String,
    ): Boolean = isPlaybackLoading && currentVideoId == requestedVideoId
}
