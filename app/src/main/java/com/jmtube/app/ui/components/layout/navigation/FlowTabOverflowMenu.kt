package com.jmtube.app.ui.components.layout.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * Splits [tabs] into the ones a navigation surface with room for [capacity] items shows and the
 * ones behind its "More" item, which takes the last slot.
 */
internal fun splitTabsForOverflow(
    tabs: List<FlowTab>,
    capacity: Int,
): Pair<List<FlowTab>, List<FlowTab>> {
    if (tabs.size <= capacity) return tabs to emptyList()
    val shown = tabs.take((capacity - 1).coerceAtLeast(0))
    return shown to tabs.drop(shown.size)
}

/** The menu a navigation surface's "More" item opens, listing the tabs it had no room for. */
@Composable
internal fun FlowTabOverflowMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    tabs: List<FlowTab>,
    selectedTab: FlowTab?,
    onTabSelected: (FlowTab) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            DropdownMenuItem(
                text = { Text(text = stringResource(tab.labelRes)) },
                onClick = {
                    onDismissRequest()
                    onTabSelected(tab)
                },
                leadingIcon = {
                    Icon(
                        imageVector = tab.icon(selected = isSelected),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
            )
        }
    }
}
