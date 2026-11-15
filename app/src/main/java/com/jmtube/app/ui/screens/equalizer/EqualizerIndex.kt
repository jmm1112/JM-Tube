package com.jmtube.app.ui.screens.equalizer

import com.jmtube.app.R
import com.jmtube.app.ui.components.settings.SettingEntry
import com.jmtube.app.ui.components.settings.SettingsDestination

/** The equalizer options Settings can find and mark as new. */
internal object EqualizerIndex {
    val spatialSound =
        SettingEntry(
            key = "equalizer.spatial_sound",
            title = R.string.eq_spatial_sound,
            summary = R.string.eq_spatial_sound_summary,
            keywords = R.string.eq_spatial_sound_keywords,
            section = R.string.eq_settings_section,
            destination = SettingsDestination.EQUALIZER,
        )

    val all: List<SettingEntry> = listOf(spatialSound)
}
