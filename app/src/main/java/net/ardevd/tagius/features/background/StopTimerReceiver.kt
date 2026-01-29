package net.ardevd.tagius.features.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class StopTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Launch a Coroutine to call Repository -> Stop Timer
        // Then cancel the notification
        NotificationManagerCompat.from(context).cancel(1001)
    }
}