package com.jmtube.app.ui.screens.update

import com.jmtube.app.data.update.AppRelease

const val UPDATE_ROUTE = "update"

/** What the update page's action bar offers next. */
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
        val message: String = "",
    ) : UpdateStage
}
