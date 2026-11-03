package com.jmtube.app.ui.screens.settings.security

import com.jmtube.app.security.AppLockPreferences
import com.jmtube.app.ui.screens.settings.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppLockViewModel
    @Inject
    constructor(
        val appLockPreferences: AppLockPreferences,
    ) : SettingsViewModel() {
        val enabled = appLockPreferences.enabled.asState(false)
        val biometricPreferred = appLockPreferences.biometricPreferred.asState(true)
        val timeoutMinutes = appLockPreferences.timeoutMinutes.asState(AppLockPreferences.DEFAULT_TIMEOUT_MINUTES)
        val hideInRecents = appLockPreferences.hideInRecents.asState(true)

        fun setEnabled(value: Boolean) = write { appLockPreferences.setEnabled(value) }

        fun setBiometricPreferred(value: Boolean) = write { appLockPreferences.setBiometricPreferred(value) }

        fun setTimeoutMinutes(minutes: Int) = write { appLockPreferences.setTimeoutMinutes(minutes) }

        fun setHideInRecents(value: Boolean) = write { appLockPreferences.setHideInRecents(value) }

        fun lockNow() {
            appLockPreferences.lockNow()
        }
    }
