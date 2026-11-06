package com.jmtube.app.ui.screens.update

import com.jmtube.app.data.update.UpdateDownload
import com.jmtube.app.data.update.UpdateFailure

const val UPDATE_ROUTE = "update"

sealed interface UpdateStage {
    data object Available : UpdateStage

    data class Downloading(
        val progress: Float?,
    ) : UpdateStage

    data object Verifying : UpdateStage

    data object NeedsPermission : UpdateStage

    data object Ready : UpdateStage

    data object Installing : UpdateStage

    data class Failed(
        val reason: UpdateFailure,
    ) : UpdateStage
}

internal fun updateStage(
    download: UpdateDownload,
    canInstall: Boolean,
    installing: Boolean,
    installFailed: Boolean,
): UpdateStage =
    when (download) {
        UpdateDownload.Idle -> UpdateStage.Available
        is UpdateDownload.Running -> UpdateStage.Downloading(download.progress)
        UpdateDownload.Verifying -> UpdateStage.Verifying
        is UpdateDownload.Failed -> UpdateStage.Failed(download.reason)
        UpdateDownload.Ready -> {
            when {
                !canInstall -> UpdateStage.NeedsPermission
                installFailed -> UpdateStage.Failed(UpdateFailure.INSTALL)
                installing -> UpdateStage.Installing
                else -> UpdateStage.Ready
            }
        }
    }
