package com.jmtube.app.innertube.models.body

import com.jmtube.app.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)
