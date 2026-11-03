package com.jmtube.app.ui.screens.settings.security

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jmtube.app.R
import com.jmtube.app.security.AppLockPreferences
import com.jmtube.app.security.NativeAuthHelper
import com.jmtube.app.ui.components.settings.SettingsPage
import com.jmtube.app.ui.components.settings.choice
import com.jmtube.app.ui.components.settings.nav
import com.jmtube.app.ui.components.settings.switch
import com.jmtube.app.ui.components.shared.FlowChoice
import com.jmtube.app.ui.components.shared.FlowChoiceDialog
import com.jmtube.app.ui.screens.settings.index.AppLockIndex
import kotlinx.coroutines.launch

@Composable
internal fun AppLockScreen(
    onBack: (() -> Unit)?,
    highlight: String?,
    viewModel: AppLockViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val biometricPreferred by viewModel.biometricPreferred.collectAsStateWithLifecycle()
    val timeoutMinutes by viewModel.timeoutMinutes.collectAsStateWithLifecycle()
    val hideInRecents by viewModel.hideInRecents.collectAsStateWithLifecycle()

    var isDeviceSecure by remember { mutableStateOf(NativeAuthHelper.isDeviceSecure(context)) }
    var isBiometricAvailable by remember { mutableStateOf(NativeAuthHelper.isBiometricAvailable(context)) }
    var showTimeoutDialog by rememberSaveable { mutableStateOf(false) }
    var pendingEnableTarget by rememberSaveable { mutableStateOf<Boolean?>(null) }

    LifecycleResumeEffect(context) {
        isDeviceSecure = NativeAuthHelper.isDeviceSecure(context)
        isBiometricAvailable = NativeAuthHelper.isBiometricAvailable(context)
        onPauseOrDispose {}
    }

    val verifyEnableTitle = stringResource(R.string.app_lock_verify_enable_title)
    val verifyDisableTitle = stringResource(R.string.app_lock_verify_disable_title)
    val verifySubtitle = stringResource(R.string.app_lock_verify_settings_subtitle)
    val authFailedMsg = stringResource(R.string.app_lock_auth_failed)

    val credentialLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.appLockPreferences.setSuppressNextBackgroundLock(false)
            val target = pendingEnableTarget
            pendingEnableTarget = null
            if (result.resultCode == Activity.RESULT_OK && target != null) {
                viewModel.setEnabled(target)
            } else if (target != null) {
                scope.launch { snackbarHostState.showSnackbar(authFailedMsg) }
            }
        }

    fun handleToggleEnabled(targetValue: Boolean) {
        if (!NativeAuthHelper.isDeviceSecure(context)) {
            isDeviceSecure = false
            return
        }
        val currentActivity = activity
        if (currentActivity == null) {
            viewModel.setEnabled(targetValue)
            return
        }
        pendingEnableTarget = targetValue
        NativeAuthHelper.authenticate(
            activity = currentActivity,
            title = if (targetValue) verifyEnableTitle else verifyDisableTitle,
            subtitle = verifySubtitle,
            preferBiometric = biometricPreferred,
            launchCredentialIntent = { intent ->
                viewModel.appLockPreferences.setSuppressNextBackgroundLock(true)
                credentialLauncher.launch(intent)
            },
            onSuccess = {
                pendingEnableTarget = null
                viewModel.setEnabled(targetValue)
            },
            onFailureOrCancel = {
                pendingEnableTarget = null
                scope.launch { snackbarHostState.showSnackbar(authFailedMsg) }
            },
        )
    }

    SettingsPage(
        title = stringResource(R.string.settings_app_lock_title),
        onBack = onBack,
        highlight = highlight,
        snackbarHostState = snackbarHostState,
    ) {
        item("app_lock.status_banner") {
            DeviceSecurityBanner(
                isDeviceSecure = isDeviceSecure,
                isBiometricAvailable = isBiometricAvailable,
                onOpenSecuritySettings = {
                    runCatching {
                        context.startActivity(NativeAuthHelper.securitySettingsIntent())
                    }
                },
            )
        }

        group(key = "app_lock.main", header = R.string.app_lock_section_security) {
            switch(
                entry = AppLockIndex.enable,
                checked = enabled && isDeviceSecure,
                onCheckedChange = ::handleToggleEnabled,
                enabled = isDeviceSecure,
                icon = Icons.Outlined.Lock,
            )
            if (enabled && isDeviceSecure) {
                switch(
                    entry = AppLockIndex.useBiometrics,
                    checked = biometricPreferred,
                    onCheckedChange = viewModel::setBiometricPreferred,
                    icon = Icons.Outlined.Fingerprint,
                )
                choice(
                    entry = AppLockIndex.timeout,
                    onClick = { showTimeoutDialog = true },
                    icon = Icons.Outlined.LockClock,
                ) {
                    timeoutLabel(timeoutMinutes)
                }
                switch(
                    entry = AppLockIndex.hideRecents,
                    checked = hideInRecents,
                    onCheckedChange = viewModel::setHideInRecents,
                    icon = Icons.Outlined.VisibilityOff,
                )
                nav(
                    entry = AppLockIndex.lockNow,
                    icon = Icons.Outlined.Shield,
                    showChevron = false,
                    onClick = viewModel::lockNow,
                )
            }
        }
    }

    if (showTimeoutDialog) {
        FlowChoiceDialog(
            title = stringResource(R.string.app_lock_timeout_title),
            description = stringResource(R.string.app_lock_timeout_dialog_description),
            icon = Icons.Outlined.LockClock,
            options =
                AppLockPreferences.TIMEOUT_OPTIONS_MINUTES.map { minutes ->
                    FlowChoice(minutes, timeoutLabel(minutes))
                },
            selected = timeoutMinutes,
            onSelect = viewModel::setTimeoutMinutes,
            onDismiss = { showTimeoutDialog = false },
        )
    }
}

@Composable
private fun DeviceSecurityBanner(
    isDeviceSecure: Boolean,
    isBiometricAvailable: Boolean,
    onOpenSecuritySettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    if (!isDeviceSecure) {
        ElevatedCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("app_lock_insecure_device_card"),
            colors =
                CardDefaults.elevatedCardColors(
                    containerColor = colors.errorContainer,
                    contentColor = colors.onErrorContainer,
                ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.app_lock_device_not_secure_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.app_lock_device_not_secure_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onOpenSecuritySettings,
                    modifier = Modifier.testTag("app_lock_open_security_button"),
                ) {
                    Text(text = stringResource(R.string.app_lock_open_security_settings))
                }
            }
        }
    } else {
        ElevatedCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("app_lock_secure_device_card"),
            colors =
                CardDefaults.elevatedCardColors(
                    containerColor = colors.secondaryContainer.copy(alpha = 0.65f),
                    contentColor = colors.onSecondaryContainer,
                ),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (isBiometricAvailable) Icons.Outlined.Fingerprint else Icons.Outlined.Security,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text =
                        stringResource(
                            if (isBiometricAvailable) {
                                R.string.app_lock_status_ready_biometric
                            } else {
                                R.string.app_lock_status_ready_credential
                            },
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun timeoutLabel(minutes: Int): String =
    when (minutes) {
        0 -> stringResource(R.string.app_lock_timeout_immediately)
        1 -> stringResource(R.string.app_lock_timeout_1_min)
        5 -> stringResource(R.string.app_lock_timeout_5_min)
        15 -> stringResource(R.string.app_lock_timeout_15_min)
        30 -> stringResource(R.string.app_lock_timeout_30_min)
        else -> stringResource(R.string.app_lock_timeout_immediately)
    }
