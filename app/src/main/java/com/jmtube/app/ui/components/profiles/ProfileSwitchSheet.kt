package com.jmtube.app.ui.components.profiles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jmtube.app.R
import com.jmtube.app.data.profile.Profile
import com.jmtube.app.data.profile.ProfileRegistry
import com.jmtube.app.data.profile.displayName
import com.jmtube.app.ui.components.layout.LocalFlowSnackbarHostState
import com.jmtube.app.ui.components.settings.SettingsDestination
import com.jmtube.app.ui.components.settings.SettingsTarget
import com.jmtube.app.ui.components.shared.FlowNavRow
import com.jmtube.app.ui.components.shared.FlowRowGroup
import com.jmtube.app.ui.components.shared.FlowSectionHeader
import com.jmtube.app.ui.components.shared.FlowSegmentedGap
import com.jmtube.app.ui.components.shared.flowRowGroupShape
import com.jmtube.app.ui.components.shared.quickactions.QuickActionRow
import com.jmtube.app.ui.components.shared.quickactions.QuickActionsGroup
import com.jmtube.app.ui.components.shared.quickactions.QuickActionsSheet

private val TopBarAvatarSize = 32.dp
private val FooterPadding = 20.dp

/** Tells why a switch through [viewModel] didn't happen. */
@Composable
fun SwitchRefusals(viewModel: ProfileSwitchViewModel) {
    val refused by viewModel.refused.collectAsStateWithLifecycle()
    val snackbarHostState = LocalFlowSnackbarHostState.current
    val refusedText = refused?.let { stringResource(switchRefusalText(it.reason)) }
    LaunchedEffect(refused) {
        if (refusedText == null) return@LaunchedEffect
        try {
            snackbarHostState?.showSnackbar(refusedText)
        } finally {
            viewModel.consumeRefused()
        }
    }
}

/**
 * The running profile's avatar as a top bar action, opening the way to switch, add or manage profiles. It shows with
 * a single profile too, so the feature can be found.
 */
@Composable
fun ProfileTopBarAction(
    onOpenSettings: (SettingsTarget) -> Unit,
    viewModel: ProfileSwitchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val running = state?.profiles?.firstOrNull { it.id == viewModel.runningId } ?: return
    var open by rememberSaveable { mutableStateOf(false) }
    val description = stringResource(R.string.profiles_action_description, running.displayName(context))

    IconButton(onClick = { open = true }, modifier = Modifier.semantics { contentDescription = description }) {
        ProfileAvatar(running, size = TopBarAvatarSize)
    }

    SwitchRefusals(viewModel)

    if (open) {
        ProfileSwitchSheet(
            profiles = state?.profiles.orEmpty(),
            runningId = viewModel.runningId,
            onSwitch = viewModel::switchTo,
            onAdd = { onOpenSettings(SettingsTarget(SettingsDestination.PROFILE_EDIT)) },
            onManage = { onOpenSettings(SettingsTarget(SettingsDestination.PROFILES)) },
            onDismiss = { open = false },
        )
    }
}

@Composable
internal fun ProfileSwitchSheet(
    profiles: List<Profile>,
    runningId: String,
    onSwitch: (String) -> Unit,
    onAdd: () -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val running = profiles.firstOrNull { it.id == runningId }
    val others = profiles.filterNot { it.id == runningId }
    val atLimit = profiles.size >= ProfileRegistry.MAX_PROFILES
    var showSwitchList by rememberSaveable { mutableStateOf(false) }

    QuickActionsSheet(onDismiss = onDismiss, expandFully = true) { sheet ->
        if (running != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProfileAvatar(profile = running, size = 100.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = running.displayName(context),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.profiles_in_use),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        QuickActionsGroup(
            title = null,
            rows =
                buildList {
                    if (others.isNotEmpty()) {
                        add(
                            QuickActionRow("profiles.switch_toggle") { shape ->
                                FlowNavRow(
                                    title = if (showSwitchList) stringResource(R.string.profiles_hide_others) else stringResource(R.string.profiles_show_others),
                                    onClick = { showSwitchList = !showSwitchList },
                                    leadingIcon = Icons.Outlined.SwitchAccount,
                                    showChevron = false,
                                    trailingContent = {
                                        Icon(
                                            imageVector = if (showSwitchList) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = null,
                                        )
                                    },
                                    shape = shape,
                                )
                            },
                        )
                    }
                    add(
                        QuickActionRow("profiles.add") { shape ->
                            FlowNavRow(
                                title = stringResource(R.string.profiles_add),
                                supportingText = stringResource(R.string.profiles_limit, ProfileRegistry.MAX_PROFILES).takeIf { atLimit },
                                onClick = {
                                    sheet.close()
                                    onAdd()
                                },
                                enabled = !atLimit,
                                showChevron = false,
                                leadingIcon = Icons.Outlined.PersonAdd,
                                shape = shape,
                            )
                        },
                    )
                    add(
                        QuickActionRow("profiles.manage") { shape ->
                            FlowNavRow(
                                title = stringResource(R.string.profiles_manage),
                                onClick = {
                                    sheet.close()
                                    onManage()
                                },
                                leadingIcon = Icons.Outlined.ManageAccounts,
                                shape = shape,
                            )
                        },
                    )
                },
        )

        AnimatedVisibility(
            visible = showSwitchList && others.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FlowSectionHeader(text = stringResource(R.string.profiles_title))
                FlowRowGroup {
                    others.forEachIndexed { index, profile ->
                        FlowNavRow(
                            title = profile.displayName(context),
                            onClick = {
                                sheet.close()
                                onSwitch(profile.id)
                            },
                            showChevron = false,
                            leadingContent = { ProfileAvatar(profile = profile, size = 36.dp) },
                            shape = flowRowGroupShape(index = index, count = others.size),
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.profiles_sheet_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(FooterPadding),
        )
    }
}
