package net.ardevd.tagius.features.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import net.ardevd.tagius.MainActivity
import net.ardevd.tagius.R
import net.ardevd.tagius.core.data.TokenManager
import net.ardevd.tagius.core.network.RetrofitClient

class ZombieCheckWorker(
    val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val apiService = RetrofitClient.getInstance(applicationContext)

            val now = System.currentTimeMillis() / 1000
            val startOfDay = now - (24 * 60 * 60) // Look back 24h just in case
            val timeRangeString = "$startOfDay-$now"
            val response = apiService.getRecords(timeRangeString, running = 1)

            if (response.records.isNotEmpty()) {
                val record = response.records[0]

                // Check if we've notified on this before
                val tokenManager = TokenManager(context)
                val lastNotifiedId = tokenManager.getLastZombieIdBlocking()

                if (record.key != lastNotifiedId) {
                    val durationHours = (now - record.startTime) / 3600
                    // CHECK THRESHOLD (10 hours)
                    if (durationHours >= 10) {
                        sendNotification(durationHours.toInt())
                        // Save the record, to avoid future duplicate notifications
                        tokenManager.saveLastZombieId(record.key)
                    }
                }
            }

            Result.success()
        } catch (_: Exception) {
            // If network fails, just retry later
            Result.retry()
        }
    }

    private fun sendNotification(hours: Int) {
        val channelId = "zombie_alert"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel (Safe to call repeatedly)
        val channel =
            NotificationChannel(channelId, "Timer Alerts", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
        val descText = context.getString(R.string.zombie_still_working_desc, hours)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_timer) // Make sure you have this icon
            .setContentTitle(context.getString(R.string.zombie_still_working))
            .setContentText(descText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}