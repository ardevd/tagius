package net.ardevd.tagius.features.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import net.ardevd.tagius.core.data.TimeTaggerRecord
import net.ardevd.tagius.core.network.RetrofitClient
import net.ardevd.tagius.features.records.data.RecordsRepository

class StopTimerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val key = inputData.getString(TimerNotificationManager.EXTRA_KEY) ?: return Result.failure()
        val description = inputData.getString(TimerNotificationManager.EXTRA_DESCRIPTION) ?: ""
        val startTime = inputData.getLong(TimerNotificationManager.EXTRA_START_TIME, 0L)

        return try {
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
            
            // Cancel notification after successful stop
            TimerNotificationManager.cancelTimerNotification(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
