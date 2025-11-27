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

    suspend fun deleteRecord(record: TimeTaggerRecord): Boolean {
        val now = System.currentTimeMillis() / 1000

        // The Timetagger convention for deletion:
        // Prefix "HIDDEN" to description
        // Update 'mt' (modified time)
        val newDescription = "HIDDEN ${record.description}"

        val deletedRecord = record.copy(
            description = newDescription,
            modifiedTime = now
        )

        return try {
            val response = apiService.updateRecords(listOf(deletedRecord))
            response.accepted.contains(record.key)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateRecord(record: TimeTaggerRecord, newDescription: String): Boolean {
        val now = System.currentTimeMillis() / 1000

        val updatedRecord = record.copy(
            description = newDescription,
            modifiedTime = now
        )

        return try {
            val response = apiService.updateRecords(listOf(updatedRecord))
            response.accepted.contains(record.key)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun stopRecord(record: TimeTaggerRecord): Boolean {
        val now = System.currentTimeMillis() / 1000

        val updatedRecord = record.copy(
            endTime = now,
            modifiedTime = now
        )

        return try {

            val response = apiService.updateRecords(listOf(updatedRecord))

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