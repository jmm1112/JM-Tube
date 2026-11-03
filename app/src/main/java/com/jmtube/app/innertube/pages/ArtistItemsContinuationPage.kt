package com.jmtube.app.innertube.pages

import com.jmtube.app.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
