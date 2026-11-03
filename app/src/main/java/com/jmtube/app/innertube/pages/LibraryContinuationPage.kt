package com.jmtube.app.innertube.pages

import com.jmtube.app.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
