package app.ambr.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.ambr.AmbrApp
import app.ambr.MainActivity
import app.ambr.R
import app.ambr.domain.Format
import app.ambr.domain.Reminders
import app.ambr.domain.Warranty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Runs once a day, mid-morning, and posts any warranty reminders that are due. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as AmbrApp).container
        // Quiet hours: never post between 10 pm and 8 am, even if the job runs late.
        val now = LocalTime.now()
        if (now.isBefore(LocalTime.of(8, 0)) || now.isAfter(LocalTime.of(22, 0))) return Result.success()
        val today = LocalDate.now()
        val first = c.settings.firstReminderDays.value
        val second = c.settings.secondReminder.value
        c.items.activeOnce().forEach { d ->
            d.coverages.forEach { cov ->
                val stage = Reminders.due(cov, today, first, second) ?: return@forEach
                val end = Warranty.end(cov) ?: return@forEach
                Notifications.warrantyEnding(applicationContext, d.item.id, d.item.name, cov.label, end, stage)
                c.items.updateCoverage(cov.copy(remindersSent = stage))
            }
        }
        return Result.success()
    }

    companion object {
        private const val NAME = "warranty-reminders"

        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(10, 30)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

object Notifications {
    private const val CHANNEL = "warranty"
    const val EXTRA_ITEM = "itemId"
    const val ACTION_ALL_GOOD = "app.ambr.ALL_GOOD"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "Warranty reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "At most two notes before a warranty ends"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun openItemIntent(context: Context, itemId: Long): PendingIntent = PendingIntent.getActivity(
        context, itemId.toInt(),
        Intent(context, MainActivity::class.java).putExtra(EXTRA_ITEM, itemId).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun warrantyEnding(context: Context, itemId: Long, name: String, part: String, end: LocalDate, stage: Int) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= 33
        ) return
        val what = if (part == "Full warranty") "warranty" else "${part.lowercase()} warranty"
        val allGood = PendingIntent.getBroadcast(
            context, itemId.toInt(),
            Intent(context, ReminderActionReceiver::class.java).setAction(ACTION_ALL_GOOD).putExtra(EXTRA_ITEM, itemId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_bell)
            .setColor(0xFFEF9426.toInt())
            .setContentTitle("Your $name $what ends on ${Format.dateShort(end)}")
            .setContentText(if (stage == 1) "Anything not working right? Get it checked for free now." else "One week left. After that, repairs cost money.")
            .setContentIntent(openItemIntent(context, itemId))
            .setAutoCancel(true)
            .addAction(0, "Book a check-up", openItemIntent(context, itemId))
            .apply { if (stage == 1) addAction(0, "All good", allGood) }
            .build()
        NotificationManagerCompat.from(context).notify(itemId.toInt(), n)
    }
}

/** "All good" on the first note: no second reminder for this item. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(Notifications.EXTRA_ITEM, 0L)
        if (intent.action != Notifications.ACTION_ALL_GOOD || id == 0L) return
        NotificationManagerCompat.from(context).cancel(id.toInt())
        val pending = goAsync()
        val repo = (context.applicationContext as AmbrApp).container.items
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repo.item(id).first()?.coverages?.forEach { repo.updateCoverage(it.copy(reminderDismissed = true)) }
            } finally {
                pending.finish()
            }
        }
    }
}
