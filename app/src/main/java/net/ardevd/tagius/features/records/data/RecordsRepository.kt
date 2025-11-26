package net.ardevd.tagius.features.records.data

import android.util.Log
import net.ardevd.tagius.core.data.TimeTaggerRecord
import net.ardevd.tagius.core.network.TimeTaggerApiService
import net.ardevd.tagius.core.utils.KeyGenerator


class RecordsRepository(service: TimeTaggerApiService) {

    private val apiService = service

    suspend fun startRecord(description: String): Boolean {
        val now = System.currentTimeMillis() / 1000
        val newKey = KeyGenerator.generateKey()

        val newRecord = TimeTaggerRecord(
            key = newKey,
            startTime = now,
            endTime = now,
            modifiedTime = now,
            description = description,
            serverTime = 0.0
        )

        return try {
            // We reuse the same updateRecords (@PUT) endpoint
            val response = apiService.updateRecords(listOf(newRecord))

            // Success if the server accepted our new key
            response.accepted.contains(newKey)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun stopRecord(record: TimeTaggerRecord): Boolean {
        // 1. Calculate "Now" in Unix seconds
        val now = System.currentTimeMillis() / 1000

        // 2. Create a copy of the record with updated timestamps
        // We update 't2' to stop it, and 'mt' to mark it as modified.
        val updatedRecord = record.copy(
            endTime = now,
            modifiedTime = now
        )

        return try {
            // 3. Send it to the API (wrapped in a list)
            val response = apiService.updateRecords(listOf(updatedRecord))

            // 4. Return true if our record key is in the 'accepted' list
            response.accepted.contains(record.key)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchRecords(start: Long, end: Long): List<TimeTaggerRecord> {
        // Construct the timerange string as required by the API
        val timeRangeString = "$start-$end"

        return try {
            val response = apiService.getRecords(timerange = timeRangeString)
            response.records
                .filterNot { it.description.startsWith("HIDDEN") }
                .sortedByDescending { it.startTime }
        } catch (e: Exception) {

            Log.d("Tagius","Error fetching records: ${e.message}")
            emptyList()
        }
    }
}