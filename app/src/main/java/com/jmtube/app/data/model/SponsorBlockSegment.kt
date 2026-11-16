package com.jmtube.app.data.model

import com.google.gson.annotations.SerializedName

data class SponsorBlockSegment(
    @SerializedName("category") val category: String,
    @SerializedName("segment") val segment: List<Float>, // [start, end]
    @SerializedName("UUID") val uuid: String,
    @SerializedName("actionType") val actionType: String // "skip", "mute", etc. (usually skip)
) {
    val startTime: Float get() = if (segment.isNotEmpty()) segment[0] else 0f
    val endTime: Float get() = if (segment.size > 1) segment[1] else 0f

    /** A highlight: one moment, as opposed to a range or a whole-video label at `[0, 0]`. */
    val isPoint: Boolean get() = category == SponsorBlockCategories.HIGHLIGHT
}
