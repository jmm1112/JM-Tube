package com.jmtube.app.innertube.models.body

import com.jmtube.app.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class ReelBody(
    val context: Context,
    val sequenceParams: String,
)
