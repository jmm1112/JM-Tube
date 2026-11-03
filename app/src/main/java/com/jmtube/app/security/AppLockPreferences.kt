package com.jmtube.app.security

import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.jmtube.app.data.local.devicePreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages JM Tube's App Lock settings and process-level lock state.
 * Uses Android's native Biometric / Screen Lock credential system.
 */
@Singleton
class AppLockPreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        val enabled: Flow<Boolean> =
            context.devicePreferencesDataStore.data
                .map { it[KEY_ENABLED] ?: false }
                .distinctUntilChanged()

        val biometricPreferred: Flow<Boolean> =
            context.devicePreferencesDataStore.data
                .map { it[KEY_BIOMETRIC_PREFERRED] ?: true }
                .distinctUntilChanged()

        val timeoutMinutes: Flow<Int> =
            context.devicePreferencesDataStore.data
                .map { it[KEY_TIMEOUT_MINUTES] ?: DEFAULT_TIMEOUT_MINUTES }
                .distinctUntilChanged()

        val hideInRecents: Flow<Boolean> =
            context.devicePreferencesDataStore.data
                .map { it[KEY_HIDE_IN_RECENTS] ?: true }
                .distinctUntilChanged()

        private val _isLocked = MutableStateFlow(false)
        val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

        @Volatile
        private var cachedEnabled: Boolean = false

        @Volatile
        private var cachedTimeoutMinutes: Int = DEFAULT_TIMEOUT_MINUTES

        @Volatile
        private var initialCheckDone: Boolean = false

        @Volatile
        private var lastBackgroundRealtimeMs: Long = 0L

        @Volatile
        private var suppressBackgroundLock: Boolean = false

        init {
            scope.launch {
                context.devicePreferencesDataStore.data.collect { prefs ->
                    val isEnabled = prefs[KEY_ENABLED] ?: false
                    val timeout = prefs[KEY_TIMEOUT_MINUTES] ?: DEFAULT_TIMEOUT_MINUTES
                    cachedEnabled = isEnabled
                    cachedTimeoutMinutes = timeout
                    if (!initialCheckDone) {
                        initialCheckDone = true
                        if (isEnabled && NativeAuthHelper.isDeviceSecure(context)) {
                            _isLocked.value = true
                        }
                    } else if (!isEnabled) {
                        _isLocked.value = false
                    }
                }
            }
        }

        suspend fun setEnabled(value: Boolean) {
            cachedEnabled = value
            context.devicePreferencesDataStore.edit { prefs ->
                prefs[KEY_ENABLED] = value
            }
            if (!value) {
                _isLocked.value = false
                lastBackgroundRealtimeMs = 0L
            }
        }

        suspend fun setBiometricPreferred(value: Boolean) {
            context.devicePreferencesDataStore.edit { prefs ->
                prefs[KEY_BIOMETRIC_PREFERRED] = value
            }
        }

        suspend fun setTimeoutMinutes(minutes: Int) {
            cachedTimeoutMinutes = minutes
            context.devicePreferencesDataStore.edit { prefs ->
                prefs[KEY_TIMEOUT_MINUTES] = minutes
            }
        }

        suspend fun setHideInRecents(value: Boolean) {
            context.devicePreferencesDataStore.edit { prefs ->
                prefs[KEY_HIDE_IN_RECENTS] = value
            }
        }

        fun setSuppressNextBackgroundLock(suppress: Boolean) {
            suppressBackgroundLock = suppress
        }

        fun onAppStart() {
            if (suppressBackgroundLock) {
                suppressBackgroundLock = false
                return
            }
            if (!cachedEnabled || !NativeAuthHelper.isDeviceSecure(context)) {
                _isLocked.value = false
                return
            }
            val bgTime = lastBackgroundRealtimeMs
            if (bgTime > 0L) {
                val elapsedMs = SystemClock.elapsedRealtime() - bgTime
                val requiredMs = cachedTimeoutMinutes.coerceAtLeast(0) * 60_000L
                if (elapsedMs >= requiredMs) {
                    _isLocked.value = true
                }
                lastBackgroundRealtimeMs = 0L
            }
        }

        fun onAppStop(isInPipMode: Boolean) {
            if (isInPipMode || suppressBackgroundLock) return
            if (!cachedEnabled || !NativeAuthHelper.isDeviceSecure(context)) return
            lastBackgroundRealtimeMs = SystemClock.elapsedRealtime()
            if (cachedTimeoutMinutes <= 0) {
                _isLocked.value = true
            }
        }

        fun unlock() {
            lastBackgroundRealtimeMs = 0L
            _isLocked.value = false
        }

        fun lockNow() {
            if (NativeAuthHelper.isDeviceSecure(context)) {
                _isLocked.value = true
            }
        }

        companion object {
            const val DEFAULT_TIMEOUT_MINUTES = 0
            val TIMEOUT_OPTIONS_MINUTES = listOf(0, 1, 5, 15, 30)

            private val KEY_ENABLED = booleanPreferencesKey("app_lock_enabled")
            private val KEY_BIOMETRIC_PREFERRED = booleanPreferencesKey("app_lock_biometric_preferred")
            private val KEY_TIMEOUT_MINUTES = intPreferencesKey("app_lock_timeout_minutes")
            private val KEY_HIDE_IN_RECENTS = booleanPreferencesKey("app_lock_hide_recents")
        }
    }
