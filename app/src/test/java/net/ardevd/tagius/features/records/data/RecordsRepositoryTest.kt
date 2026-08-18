package net.ardevd.tagius.features.records.data

import kotlinx.coroutines.runBlocking
import net.ardevd.tagius.core.data.TimeTaggerPutResponse
import net.ardevd.tagius.core.data.TimeTaggerRecord
import net.ardevd.tagius.core.data.TimeTaggerRecordResponse
import net.ardevd.tagius.core.data.TimeTaggerVersionResponse
import net.ardevd.tagius.core.network.TimeTaggerApiService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordsRepositoryTest {

    @Test
    fun startRecord_usesSpecifiedEarlierStartForRunningRecord() = runBlocking {
        val apiService = RecordingApiService()
        val repository = RecordsRepository(apiService)
        val earlierStart = 1_723_456_789L

        val success = repository.startRecord("Planning #work", earlierStart)

        assertTrue(success)
        val record = apiService.updatedRecords.single()
        assertEquals(earlierStart, record.startTime)
        assertEquals(earlierStart, record.endTime)
        assertEquals("Planning #work", record.description)
        assertTrue(record.modifiedTime >= earlierStart)
    }

    @Test
    fun startRecord_defaultsToCurrentTime() = runBlocking {
        val apiService = RecordingApiService()
        val repository = RecordsRepository(apiService)
        val beforeStart = System.currentTimeMillis() / 1000

        val success = repository.startRecord("Current task")

        val afterStart = System.currentTimeMillis() / 1000
        assertTrue(success)
        val record = apiService.updatedRecords.single()
        assertTrue(record.startTime in beforeStart..afterStart)
        assertEquals(record.startTime, record.endTime)
        assertTrue(record.modifiedTime in beforeStart..afterStart)
    }

    private class RecordingApiService : TimeTaggerApiService {
        var updatedRecords: List<TimeTaggerRecord> = emptyList()

        override suspend fun updateRecords(records: List<TimeTaggerRecord>): TimeTaggerPutResponse {
            updatedRecords = records
            return TimeTaggerPutResponse(
                accepted = records.map { it.key },
                failed = emptyList(),
                errors = null
            )
        }

        override suspend fun getSettings(): Any = error("Not used")

        override suspend fun getVersion(): TimeTaggerVersionResponse = error("Not used")

        override suspend fun getRecords(
            timerange: String,
            running: Int?,
            hidden: Int?,
            tag: String?
        ): TimeTaggerRecordResponse = error("Not used")
    }
}
