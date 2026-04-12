package net.ardevd.tagius.features.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TimerNotificationManager.ACTION_STOP) {
            val key = intent.getStringExtra(TimerNotificationManager.EXTRA_KEY) ?: return
            val description = intent.getStringExtra(TimerNotificationManager.EXTRA_DESCRIPTION) ?: ""
            val startTime = intent.getLongExtra(TimerNotificationManager.EXTRA_START_TIME, 0L)

            val data = Data.Builder()
                .putString(TimerNotificationManager.EXTRA_KEY, key)
                .putString(TimerNotificationManager.EXTRA_DESCRIPTION, description)
                .putLong(TimerNotificationManager.EXTRA_START_TIME, startTime)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<StopTimerWorker>()
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
