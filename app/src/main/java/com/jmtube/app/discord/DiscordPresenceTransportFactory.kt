package com.jmtube.app.discord

import android.content.Context
import okhttp3.OkHttpClient

interface DiscordPresenceTransportFactory {
    fun create(
        context: Context,
        okHttpClient: OkHttpClient,
        tokenStore: DiscordTokenStore,
    ): DiscordPresenceTransport
}
