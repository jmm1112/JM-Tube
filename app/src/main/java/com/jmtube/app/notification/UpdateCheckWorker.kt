package com.jmtube.app.notification

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jmtube.app.BuildConfig
import com.jmtube.app.data.local.LocalDataManager
import com.jmtube.app.data.update.UpdateRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

private const val TAG = "UpdateCheckWorker"
private const val WORK_NAME = "update_check_work"

class UpdateCheckWorker(
    private val appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val updates = UpdateRepository(OkHttpClient(), LocalDataManager(appContext))

    companion object {
        fun schedulePeriodicCheck(context: Context, reschedule: Boolean = false) {
            if (!BuildConfig.UPDATER_ENABLED) return
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            val policy = if (reschedule) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, policy, request)
        }

        fun cancelScheduledChecks(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        if (!BuildConfig.UPDATER_ENABLED) return Result.success()
        return try {
            val release = updates.fetch()
            if (release != null) {
                NotificationHelper.showUpdateNotification(appContext, release)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Periodic update check failed", e)
            Result.retry()
        }
    }
}
