package com.jmtube.app.innertube.pages

import com.jmtube.app.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
