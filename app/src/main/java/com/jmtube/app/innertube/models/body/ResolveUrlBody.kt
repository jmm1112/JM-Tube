package com.jmtube.app.innertube.models.body

import com.jmtube.app.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class ResolveUrlBody(
    val context: Context,
    val url: String,
)
