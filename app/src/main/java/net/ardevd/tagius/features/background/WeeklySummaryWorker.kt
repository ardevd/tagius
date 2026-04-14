package net.ardevd.tagius.features.background

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
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
import net.ardevd.tagius.core.ui.tagRegexPattern

class WeeklySummaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val tokenManager = TokenManager(applicationContext)
            val token = tokenManager.getTokenBlocking()
            if (token.isNullOrBlank()) {
                return Result.success()
            }

            val apiService = RetrofitClient.getInstance(applicationContext)

            val now = System.currentTimeMillis() / 1000
            val startOfWeek = now - (7 * 24 * 60 * 60)
            val timeRangeString = "$startOfWeek-$now"
            
            val response = apiService.getRecords(timeRangeString)
            
            var totalSeconds = 0L
            val tags = mutableSetOf<String>()

            for (record in response.records) {
                val end = if (record.startTime == record.endTime) now else record.endTime
                val duration = end - record.startTime
                if (duration > 0) {
                    totalSeconds += duration
                }
                
                val matcher = tagRegexPattern.matcher(record.description)
                while (matcher.find()) {
                    tags.add(matcher.group().lowercase())
                }
            }

            if (totalSeconds > 0) {
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val durationString = "${hours}h ${minutes}m"
                sendNotification(durationString, tags.size)
            }

            Result.success()
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                Result.success() // Or failure(), but avoid retry for auth errors
            } else {
                Result.retry()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun sendNotification(durationString: String, tagsCount: Int) {
        val channelId = "weekly_summary"
        val groupId = "background_tasks"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(groupId, applicationContext.getString(R.string.notification_group_background))
        )

        val channel = NotificationChannel(
            channelId,
            applicationContext.getString(R.string.notification_channel_weekly_summary),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            group = groupId
        }
        notificationManager.createNotificationChannel(channel)
        
        val descText = applicationContext.getString(R.string.weekly_summary_desc, durationString, tagsCount)

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
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(applicationContext.getString(R.string.weekly_summary_title))
            .setContentText(descText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1003, notification)
    }
}
