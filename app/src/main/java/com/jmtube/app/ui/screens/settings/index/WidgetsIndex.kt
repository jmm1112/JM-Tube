package com.jmtube.app.ui.screens.settings.index

import com.jmtube.app.R
import com.jmtube.app.ui.components.settings.SettingEntry
import com.jmtube.app.ui.components.settings.SettingsDestination
import com.jmtube.app.widget.core.FlowWidgetEntry
import com.jmtube.app.widget.core.FlowWidgets

internal object WidgetsIndex {
    fun entryFor(widget: FlowWidgetEntry) =
        SettingEntry(
            key = "widgets.${widget.id}",
            title = widget.label,
            summary = widget.description,
            section = R.string.settings_widgets_add_header,
            keywords = R.string.settings_keywords_widgets,
            destination = SettingsDestination.WIDGETS,
        )

    val all = FlowWidgets.catalog.map(::entryFor)
}
