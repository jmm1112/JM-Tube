package com.jmtube.app.ui.screens.update

import android.content.Intent
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.jmtube.app.data.update.AppRelease
import com.jmtube.app.data.update.UpdateDownload
import com.jmtube.app.data.update.UpdateFailure
import com.jmtube.app.data.update.UpdateRepository
import com.jmtube.app.updater.AppUpdateInstaller
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.jmtube.app.data.update.ReleaseNotes
import com.jmtube.app.data.update.parseReleaseNotes

private const val STOP_TIMEOUT_MS = 5_000L

@Immutable
data class UpdateUiState(
    val release: AppRelease? = null,
    val notes: ReleaseNotes? = null,
    val stage: UpdateStage = UpdateStage.Available,
    val loading: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UpdateViewModel
    @Inject
    constructor(
        private val updates: UpdateRepository,
        private val installer: AppUpdateInstaller,
    ) : ViewModel() {
        private val canInstall = MutableStateFlow(installer.canInstall())
        private val installing = MutableStateFlow(false)
        private val loading = MutableStateFlow(true)
        private val _release = MutableStateFlow<AppRelease?>(null)

        val state: StateFlow<UpdateUiState> =
            combine(
                _release.flatMapLatest { release ->
                    if (release == null) {
                        flowOf(UpdateUiState())
                    } else {
                        val notes = parseReleaseNotes(release.releaseNotes)
                        combine(
                            installer.state(release.versionName),
                            canInstall,
                            installing,
                            installer.installFailed,
                        ) { download, allowed, busy, failed ->
                            UpdateUiState(release, notes, updateStage(download, allowed, busy, failed))
                        }
                    }
                },
                loading,
            ) { state, isLoading -> state.copy(loading = isLoading && state.release == null) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UpdateUiState(loading = true))

        init {
            viewModelScope.launch {
                val rel = runCatching { updates.fetch() }.getOrNull()
                _release.value = rel
                loading.value = false
            }
        }

        fun primaryAction() {
            val current = state.value
            val release = current.release ?: return
            when (val stage = current.stage) {
                UpdateStage.Available -> installer.download(release)
                is UpdateStage.Failed -> if (stage.reason == UpdateFailure.INSTALL) install(release) else installer.download(release)
                UpdateStage.Ready -> install(release)
                else -> Unit
            }
        }

        fun cancelDownload() = installer.cancel()

        fun installPermissionIntent(): Intent = installer.installPermissionIntent()

        fun onResume() {
            canInstall.value = installer.canInstall()
            installing.value = false
        }

        private fun install(release: AppRelease) {
            installing.value = true
            viewModelScope.launch { if (!installer.install(release.versionName)) installing.value = false }
        }
    }
