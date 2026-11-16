package com.jmtube.app.ui.screens.sync.linked

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jmtube.app.R
import com.jmtube.app.ui.components.shared.FlowSheetHeader
import com.jmtube.app.ui.components.shared.FlowSwitchRow
import com.jmtube.app.ui.components.shared.quickactions.QuickActionRow
import com.jmtube.app.ui.components.shared.quickactions.QuickActionsGroup
import com.jmtube.app.ui.components.shared.quickactions.QuickActionsSheet
import com.jmtube.app.ui.screens.sync.COLLECTION_KEYS
import com.jmtube.app.ui.screens.sync.collectionDescription
import com.jmtube.app.ui.screens.sync.collectionIcon
import com.jmtube.app.ui.screens.sync.collectionLabel

/** A switch per collection: off leaves it out of what this device sends and takes in. */
@Composable
internal fun LinkedSyncChoiceSheet(
    leftOut: Set<String>,
    onLeftOut: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    QuickActionsSheet(onDismiss = onDismiss, expandFully = true) {
        FlowSheetHeader(
            title = stringResource(R.string.linked_sync_choice),
            subtitle = stringResource(R.string.linked_sync_choice_body),
            onClose = null,
            showDragHandle = false,
            dividerAlpha = null,
        )
        QuickActionsGroup(
            title = null,
            rows =
                COLLECTION_KEYS.map { key ->
                    QuickActionRow(key) { shape ->
                        FlowSwitchRow(
                            title = collectionLabel(key),
                            checked = key !in leftOut,
                            onCheckedChange = { on -> onLeftOut(if (on) leftOut - key else leftOut + key) },
                            supportingText = collectionDescription(key),
                            leadingIcon = collectionIcon(key),
                            shape = shape,
                        )
                    }
                },
        )
    }
}
