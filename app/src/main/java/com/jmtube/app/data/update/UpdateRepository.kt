package com.jmtube.app.data.update

import android.util.Log
import com.jmtube.app.BuildConfig
import com.jmtube.app.data.local.LocalDataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UpdateRepository"
private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/jmm1112/JM-Tube/releases/latest"

@Singleton
class UpdateRepository
    @Inject
    constructor(
        private val client: OkHttpClient,
        private val dataManager: LocalDataManager,
    ) {
        private val json = Json { ignoreUnknownKeys = true }

        suspend fun fetch(): AppRelease? = withContext(Dispatchers.IO) {
            if (!BuildConfig.UPDATER_ENABLED) return@withContext null
            try {
                val request = Request.Builder()
                    .url(GITHUB_LATEST_RELEASE_URL)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val responseBody = response.body?.string() ?: return@withContext null
                    val release = json.decodeFromString<GitHubRelease>(responseBody)

                    val tagName = release.tagName.removePrefix("v").trim()
                    if (isNewerVersion(tagName, BuildConfig.VERSION_NAME)) {
                        val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                        val downloadUrl = apkAsset?.downloadUrl ?: "https://github.com/jmm1112/JM-Tube/releases/latest"
                        dataManager.setLastUpdateCheck(System.currentTimeMillis())
                        AppRelease(
                            versionName = tagName,
                            downloadUrl = downloadUrl,
                            releaseNotes = release.body,
                            publishedAt = release.publishedAt,
                            apkSizeBytes = apkAsset?.sizeBytes ?: 0L,
                        )
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed", e)
                null
            }
        }

        private fun isNewerVersion(remote: String, current: String): Boolean {
            val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
            for (i in 0 until maxOf(remoteParts.size, currentParts.size)) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }
    }
