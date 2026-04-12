package net.ardevd.tagius.features.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import net.ardevd.tagius.core.data.TokenManager
import net.ardevd.tagius.core.network.RetrofitClient

class RestoreTimerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val tokenManager = TokenManager(applicationContext)
            
            // If there's no valid session, just return
            if (tokenManager.authTokenFlow.first().isNullOrBlank()) {
                return Result.success()
            }

            val apiService = RetrofitClient.getInstance(applicationContext)

            val now = System.currentTimeMillis() / 1000
            val startOfDay = now - (24 * 60 * 60 * 7) // Look back 7 days just in case
            val timeRangeString = "$startOfDay-$now"
            
            val response = apiService.getRecords(timeRangeString, running = 1)

            if (response.records.isNotEmpty()) {
                val record = response.records[0]
                TimerService.startService(
                    applicationContext,
                    record.description,
                    record.startTime,
                    record.key
                )
            }

            Result.success()
        } catch (_: Exception) {
            // Network or parsing error, we can retry
            Result.retry()
        }
    }
}