package com.jmtube.app.ui.screens.settings.index

import com.jmtube.app.R
import com.jmtube.app.ui.components.settings.SettingEntry
import com.jmtube.app.ui.components.settings.SettingsDestination

internal object AppLockIndex {
    private val page = SettingsDestination.APP_LOCK

    private fun entry(
        key: String,
        title: Int,
        summary: Int? = null,
        revealVia: String? = null,
    ) = SettingEntry(
        key = "app_lock.$key",
        title = title,
        summary = summary,
        section = R.string.app_lock_section_security,
        keywords = R.string.settings_keywords_app_lock,
        revealVia = revealVia,
        destination = page,
    )

    val enable =
        entry(
            key = "enable",
            title = R.string.app_lock_enable_title,
            summary = R.string.app_lock_enable_subtitle,
        )

    val useBiometrics =
        entry(
            key = "biometric",
            title = R.string.app_lock_use_biometrics_title,
            summary = R.string.app_lock_use_biometrics_subtitle,
            revealVia = "app_lock.enable",
        )

    val timeout =
        entry(
            key = "timeout",
            title = R.string.app_lock_timeout_title,
            revealVia = "app_lock.enable",
        )

    val hideRecents =
        entry(
            key = "hide_recents",
            title = R.string.app_lock_hide_recents_title,
            summary = R.string.app_lock_hide_recents_subtitle,
            revealVia = "app_lock.enable",
        )

    val lockNow =
        entry(
            key = "lock_now",
            title = R.string.app_lock_lock_now_title,
            summary = R.string.app_lock_lock_now_subtitle,
            revealVia = "app_lock.enable",
        )

    val all = listOf(enable, useBiometrics, timeout, hideRecents, lockNow)
}
