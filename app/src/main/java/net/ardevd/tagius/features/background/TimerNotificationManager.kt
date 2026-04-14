package net.ardevd.tagius.features.background

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import net.ardevd.tagius.MainActivity
import net.ardevd.tagius.R

object TimerNotificationManager {

    const val ACTION_STOP = "net.ardevd.tagius.features.background.action.STOP"
    const val EXTRA_DESCRIPTION = "EXTRA_DESCRIPTION"
    const val EXTRA_START_TIME = "EXTRA_START_TIME"
    const val EXTRA_KEY = "EXTRA_KEY"
    private const val NOTIFICATION_ID = 1002

    fun showTimerNotification(context: Context, description: String, startTime: Long, key: String) {
        val channelId = "active_timer"
        val groupId = "timers"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(groupId, context.getString(R.string.notification_group_timers))
        )

        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.notification_channel_active_timer),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            group = groupId
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(context, TimerActionReceiver::class.java).apply {
            action = ACTION_STOP
            putExtra(EXTRA_KEY, key)
            putExtra(EXTRA_DESCRIPTION, description)
            putExtra(EXTRA_START_TIME, startTime)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.timer_running))
            .setContentText(description.ifBlank { context.getString(R.string.records_no_description) })
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setUsesChronometer(true)
            .setWhen(startTime * 1000L)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, context.getString(R.string.record_stop), stopPendingIntent)
            .apply {
                try {
                    // Try to use the NotificationCompat extension if available
                    val method = this::class.java.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                    method.invoke(this, true)
                } catch (e: Exception) {
                    // Fallback to setting extras manually
                    val extras = android.os.Bundle()
                    extras.putBoolean("android.app.extra.REQUEST_PROMOTED_ONGOING", true)
                    addExtras(extras)
                }
            }
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelTimerNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
