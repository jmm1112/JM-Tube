package com.jmtube.app.service.auto

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * Fallback provider for `androidx.car.app.connection`.
 *
 * When MediaSession, Google Play Services, or Android automotive services check
 * whether a car head unit is attached on devices or emulators where the standalone
 * Android Auto host app is not installed, this provider answers the query with
 * [CONNECTION_TYPE_NOT_CONNECTED] (0), preventing the system warning:
 * "E/ActivityThread: Failed to find provider info for androidx.car.app.connection".
 */
class CarConnectionProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val cursor = MatrixCursor(arrayOf(COLUMN_CAR_CONNECTION_STATE))
        cursor.addRow(arrayOf(CONNECTION_TYPE_NOT_CONNECTED))
        return cursor
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/car_connection_state"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    private companion object {
        const val COLUMN_CAR_CONNECTION_STATE = "CarConnectionState"
        const val CONNECTION_TYPE_NOT_CONNECTED = 0
    }
}
