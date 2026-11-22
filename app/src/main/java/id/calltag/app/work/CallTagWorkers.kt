package id.calltag.app.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import id.calltag.app.CallTagApplication
import id.calltag.app.data.remote.NetworkModule
import java.util.concurrent.TimeUnit

/**
 * Worker untuk mengirim ulang antrean laporan dan sanggahan offline (`pending_reports` & `pending_disputes`)
 * dengan `Idempotency-Key` stabil saat perangkat kembali terhubung ke jaringan (T-304).
 */
class PendingReportWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? CallTagApplication ?: return Result.failure()
        NetworkModule.init(app.preferences)

        val reportsOk = runCatching { app.repository.flushPendingReports() }
        val disputesOk = runCatching { app.repository.flushPendingDisputes() }
        if (reportsOk.isFailure && disputesOk.isFailure) {
            return if (runAttemptCount < 5) Result.retry() else Result.failure()
        }

        val remaining = runCatching { app.database.pendingReportDao().countPending() }.getOrDefault(0)
        return if (remaining > 0 && runAttemptCount < 5) Result.retry() else Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "calltag_pending_outbox_worker"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val req = OneTimeWorkRequestBuilder<PendingReportWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, req)
        }
    }
}

/**
 * Worker sinkronisasi delta berkala menggunakan cursor `/api/v1/sync/changes` (T-304).
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? CallTagApplication ?: return Result.failure()
        NetworkModule.init(app.preferences)

        val syncRes = app.repository.syncDeltaChanges()
        runCatching { app.repository.flushPendingReports() }
        return if (syncRes.isSuccess) {
            Result.success()
        } else if (runAttemptCount < 3) {
            Result.retry()
        } else {
            Result.failure()
        }
    }

    companion object {
        private const val UNIQUE_PERIODIC = "calltag_periodic_delta_sync"
        private const val UNIQUE_IMMEDIATE = "calltag_immediate_delta_sync"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val req = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun enqueueNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val req = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_IMMEDIATE, ExistingWorkPolicy.REPLACE, req)
        }
    }
}

