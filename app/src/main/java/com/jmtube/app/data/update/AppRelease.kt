package com.jmtube.app.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppRelease(
    val versionName: String,
    val versionCode: Int = 0,
    val downloadUrl: String,
    val releaseNotes: String = "",
    val publishedAt: String = "",
    val apkSizeBytes: Long = 0L,
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("body") val body: String = "",
    @SerialName("published_at") val publishedAt: String = "",
    @SerialName("assets") val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
data class GitHubAsset(
    @SerialName("name") val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    @SerialName("size") val sizeBytes: Long = 0L,
)

sealed interface UpdateDownload {
    data object Idle : UpdateDownload

    data class Running(
        val progress: Float?,
    ) : UpdateDownload

    data object Verifying : UpdateDownload

    data object Ready : UpdateDownload

    data class Failed(
        val reason: UpdateFailure,
    ) : UpdateDownload
}

enum class UpdateFailure { NETWORK, STORAGE, CHECKSUM, PACKAGE, INSTALL }
