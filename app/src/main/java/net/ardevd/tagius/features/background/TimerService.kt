package net.ardevd.tagius.features.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.ardevd.tagius.MainActivity
import net.ardevd.tagius.R
import net.ardevd.tagius.core.data.TimeTaggerRecord
import net.ardevd.tagius.core.network.RetrofitClient
import net.ardevd.tagius.features.records.data.RecordsRepository

class TimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_START -> {
                val description = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""
                val startTime = intent.getLongExtra(EXTRA_START_TIME, 0L)
                val key = intent.getStringExtra(EXTRA_KEY) ?: ""
                startForegroundService(description, startTime, key)
            }
            ACTION_STOP -> {
                val key = intent.getStringExtra(EXTRA_KEY) ?: ""
                val description = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""
                val startTime = intent.getLongExtra(EXTRA_START_TIME, 0L)
                stopTimer(key, description, startTime)
            }
        }

        return START_STICKY
    }

    private fun startForegroundService(description: String, startTime: Long, key: String) {
        val channelId = "active_timer"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            getString(R.string.notification_channel_active_timer),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
            putExtra(EXTRA_KEY, key)
            putExtra(EXTRA_DESCRIPTION, description)
            putExtra(EXTRA_START_TIME, startTime)
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(getString(R.string.timer_running))
            .setContentText(description.ifBlank { getString(R.string.records_no_description) })
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setUsesChronometer(true)
            .setWhen(startTime * 1000L)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, getString(R.string.record_stop), stopPendingIntent)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopTimer(key: String, description: String, startTime: Long) {
        serviceScope.launch {
            val apiService = RetrofitClient.getInstance(applicationContext)
            val repository = RecordsRepository(apiService)

            val record = TimeTaggerRecord(
                key = key,
                startTime = startTime,
                endTime = startTime,
                modifiedTime = startTime,
                description = description,
                serverTime = 0.0
            )
            repository.stopRecord(record)
            
            stopSelf()
        }
    }

    companion object {
        private const val ACTION_START = "ACTION_START"
        private const val ACTION_STOP = "ACTION_STOP"
        private const val EXTRA_DESCRIPTION = "EXTRA_DESCRIPTION"
        private const val EXTRA_START_TIME = "EXTRA_START_TIME"
        private const val EXTRA_KEY = "EXTRA_KEY"
        private const val NOTIFICATION_ID = 1002

        fun startService(context: Context, description: String, startTime: Long, key: String) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DESCRIPTION, description)
                putExtra(EXTRA_START_TIME, startTime)
                putExtra(EXTRA_KEY, key)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TimerService::class.java)
            context.stopService(intent)
        }
    }
}