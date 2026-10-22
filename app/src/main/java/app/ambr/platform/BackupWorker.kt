package app.ambr.platform

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.ambr.AmbrApp
import java.util.concurrent.TimeUnit

/** Nightly backup to Drive while backup is on. Needs Drive access granted earlier from the Backup screen. */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as AmbrApp).container
        if (!c.settings.backupOn.value) return Result.success()
        val token = when (val a = c.backup.access()) {
            is DriveAccess.Token -> a.value
            is DriveAccess.NeedsConsent -> { c.settings.setBackupError("signin"); return Result.success() }
            is DriveAccess.Failed -> return Result.retry()
        }
        return runCatching { c.backup.backUp(token) }.fold(
            onSuccess = { c.settings.setBackupError(null); Result.success() },
            onFailure = { e -> c.settings.setBackupError(if (e.message == "quota") "quota" else "failed"); if (runAttemptCount < 2) Result.retry() else Result.success() },
        )
    }

    companion object {
        private const val NAME = "drive-backup"

        fun schedule(context: Context, on: Boolean, wifiOnly: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (!on) { wm.cancelUniqueWork(NAME); return }
            val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build())
                .build()
            wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
