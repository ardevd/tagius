package net.ardevd.tagius.features.background

import android.app.NotificationManager
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.*
import kotlinx.coroutines.runBlocking
import net.ardevd.tagius.core.data.TimeTaggerRecord
import net.ardevd.tagius.core.data.TimeTaggerRecordResponse
import net.ardevd.tagius.core.data.TokenManager
import net.ardevd.tagius.core.network.RetrofitClient
import net.ardevd.tagius.core.network.TimeTaggerApiService
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ZombieCheckWorker.
 * 
 * These tests verify the logic for:
 * - Checking running records from the API
 * - Calculating record durations
 * - Comparing against the 10-hour threshold
 * - Preventing duplicate notifications
 * - Error handling and retry logic
 */
class ZombieCheckWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var apiService: TimeTaggerApiService
    private lateinit var tokenManager: TokenManager
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setup() {
        // Mock Android and app dependencies
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
        apiService = mockk()
        tokenManager = mockk(relaxed = true)
        notificationManager = mockk(relaxed = true)

        // Mock TokenManager constructor
        mockkConstructor(TokenManager::class)
        every { anyConstructed<TokenManager>().getLastZombieIdBlocking() } returns null
        coEvery { anyConstructed<TokenManager>().saveLastZombieId(any()) } just Runs

        // Mock RetrofitClient
        mockkObject(RetrofitClient)
        every { RetrofitClient.getInstance(any()) } returns apiService

        // Mock NotificationManager
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun doWork_noRunningRecords_returnsSuccess() = runBlocking {
        // Given: API returns no running records
        val emptyResponse = TimeTaggerRecordResponse(records = emptyList())
        coEvery { apiService.getRecords(any(), running = 1) } returns emptyResponse

        // When: Worker executes
        val worker = createWorker()
        val result = worker.doWork()

        // Then: Should return success
        assertEquals(ListenableWorker.Result.success(), result)
        
        // And: Should not send any notifications
        verify(exactly = 0) { notificationManager.notify(any(), any()) }
    }

    @Test
    fun doWork_runningRecordBelowThreshold_noNotification() = runBlocking {
        // Given: A running record that has been running for 5 hours (below 10-hour threshold)
        val now = System.currentTimeMillis() / 1000
        val fiveHoursAgo = now - (5 * 3600)
        val record = createTestRecord(key = "record1", startTime = fiveHoursAgo)
        val response = TimeTaggerRecordResponse(records = listOf(record))
        
        coEvery { apiService.getRecords(any(), running = 1) } returns response

        // When: Worker executes
        val worker = createWorker()
        val result = worker.doWork()

        // Then: Should return success
        assertEquals(ListenableWorker.Result.success(), result)
        
        // And: Should not send notification (below threshold)
        verify(exactly = 0) { notificationManager.notify(any(), any()) }
    }

    @Test
    fun doWork_runningRecordExactly10Hours_sendsNotification() = runBlocking {
        // Given: A running record that has been running for exactly 10 hours (at threshold)
        val now = System.currentTimeMillis() / 1000
        val tenHoursAgo = now - (10 * 3600)
        val record = createTestRecord(key = "record1", startTime = tenHoursAgo)
        val response = TimeTaggerRecordResponse(records = listOf(record))
        
        coEvery { apiService.getRecords(any(), running = 1) } returns response

        // When: Worker executes
        val worker = createWorker()
        val result = worker.doWork()

        // Then: Should return success
        assertEquals(ListenableWorker.Result.success(), result)
        
        // And: Should send notification (at threshold)
        verify(atLeast = 1) { notificationManager.notify(any(), any()) }
        
        // And: Should save the record ID to prevent duplicates
        coVerify { anyConstructed<TokenManager>().saveLastZombieId("record1") }
    }

    @Test
    fun doWork_runningRecordAboveThreshold_sendsNotification() = runBlocking {
        // Given: A running record that has been running for 15 hours (above threshold)
        val now = System.currentTimeMillis() / 1000
        val fifteenHoursAgo = now - (15 * 3600)
        val record = createTestRecord(key = "record2", startTime = fifteenHoursAgo)
        val response = TimeTaggerRecordResponse(records = listOf(record))
        
        coEvery { apiService.getRecords(any(), running = 1) } returns response

        // When: Worker executes
        val worker = createWorker()
        val result = worker.doWork()

        // Then: Should return success
        assertEquals(ListenableWorker.Result.success(), result)
        
        // And: Should send notification (above threshold)
        verify(atLeast = 1) { notificationManager.notify(any(), any()) }
        
        // And: Should save the record ID to prevent duplicates
        coVerify { anyConstructed<TokenManager>().saveLastZombieId("record2") }
    }

    @Test
    fun doWork_duplicateRecord_noNotification() = runBlocking {
        // Given: A running record that has already been notified (same ID stored)
        val now = System.currentTimeMillis() / 1000
        val fifteenHoursAgo = now - (15 * 3600)
        val record = createTestRecord(key = "record3", startTime = fifteenHoursAgo)
        val response = TimeTaggerRecordResponse(records = listOf(record))
        
        // Mock that we've already notified on this record
        every { anyConstructed<TokenManager>().getLastZombieIdBlocking() } returns "record3"
        
        coEvery { apiService.getRecords(any(), running = 1) } returns response

        // When: Worker executes
        val worker = createWorker()
        val result = worker.doWork()

        // Then: Should return success
        assertEquals(ListenableWorker.Result.success(), result)
        
        // And: Should not send notification (duplicate)
        verify(exactly = 0) { notificationManager.notify(any(), any()) }
        
        // And: Should not update the stored ID
        coVerify(exactly = 0) { anyConstructed<TokenManager>().saveLastZombieId(any()) }
    }

    @Test
    fun doWork_newRecordAfterPreviousNotification_sendsNotification() = runBlocking {
        // Given: A new running record different from the previously notified one
        val now = System.currentTimeMillis() / 1000
        val twelveHoursAgo = now - (12 * 3600)
        val record = createTestRecord(key = "record5", startTime = twelveHoursAgo)
        val response = TimeTaggerRecordResponse(records = listOf(record))
        
        // Mock that we've previously notified on a different record
        every { anyConstructed<TokenManager>().getLastZombieIdBlocking() } returns "record4"
        
        coEvery { apiService.getRecords(any(), running = 1) } returns response

        // When: Worker executes
        val worker = createWorker()
        val result = worker.doWork()

        // Then: Should return success
        assertEquals(ListenableWorker.Result.success(), result)
        
        // And: Should send notification (new record)
        verify(atLeast = 1) { notificationManager.notify(any(), any()) }
        
        // And: Should save the new record ID
        coVerify { anyConstructed<TokenManager>().saveLastZombieId("record5") }
    }

    @Test
    fun doWork_apiException_returnsRetry() = runBlocking {
        // Given: API throws an exception (network failure)
        coEvery { apiService.getRecords(any(), running = 1) } throws Exception("Network error")

        // When: Worker executes
        val worker = createWorker()
        val result = worker.doWork()

        // Then: Should return retry (to try again later)
        assertEquals(ListenableWorker.Result.retry(), result)
        
        // And: Should not send any notifications
        verify(exactly = 0) { notificationManager.notify(any(), any()) }
    }

    @Test
    fun doWork_checksCorrectTimeRange() = runBlocking {
        // Given: No running records
        val emptyResponse = TimeTaggerRecordResponse(records = emptyList())
        coEvery { apiService.getRecords(any(), running = 1) } returns emptyResponse

        // When: Worker executes
        val worker = createWorker()
        worker.doWork()

        // Then: Should query with correct time range (24 hours lookback)
        coVerify {
            apiService.getRecords(
                match { timeRange ->
                    // Verify format is "start-end"
                    val parts = timeRange.split("-")
                    parts.size == 2 && 
                    parts[0].toLongOrNull() != null && 
                    parts[1].toLongOrNull() != null
                },
                running = 1
            )
        }
    }

    @Test
    fun doWork_durationCalculation_correctForEdgeCases() = runBlocking {
        // Test case 1: Record at exactly 9 hours 59 minutes (should not notify)
        val now = System.currentTimeMillis() / 1000
        val almostTenHours = now - (9 * 3600 + 59 * 60)
        val record1 = createTestRecord(key = "edge1", startTime = almostTenHours)
        val response1 = TimeTaggerRecordResponse(records = listOf(record1))
        
        coEvery { apiService.getRecords(any(), running = 1) } returns response1

        val worker1 = createWorker()
        val result1 = worker1.doWork()

        assertEquals(ListenableWorker.Result.success(), result1)
        verify(exactly = 0) { notificationManager.notify(any(), any()) }

        // Clear invocations for next test
        clearMocks(notificationManager, answers = false)

        // Test case 2: Record at 10 hours 1 minute (should notify)
        val justOverTenHours = now - (10 * 3600 + 60)
        val record2 = createTestRecord(key = "edge2", startTime = justOverTenHours)
        val response2 = TimeTaggerRecordResponse(records = listOf(record2))
        
        coEvery { apiService.getRecords(any(), running = 1) } returns response2

        val worker2 = createWorker()
        val result2 = worker2.doWork()

        assertEquals(ListenableWorker.Result.success(), result2)
        verify(atLeast = 1) { notificationManager.notify(any(), any()) }
    }

    @Test
    fun doWork_multipleRunningRecords_onlyChecksFirst() = runBlocking {
        // Given: Multiple running records (worker should only check the first one)
        val now = System.currentTimeMillis() / 1000
        val record1 = createTestRecord(key = "first", startTime = now - (15 * 3600))
        val record2 = createTestRecord(key = "second", startTime = now - (20 * 3600))
        val response = TimeTaggerRecordResponse(records = listOf(record1, record2))
        
        coEvery { apiService.getRecords(any(), running = 1) } returns response

        // When: Worker executes
        val worker = createWorker()
        val result = worker.doWork()

        // Then: Should return success
        assertEquals(ListenableWorker.Result.success(), result)
        
        // And: Should only save the first record's ID
        coVerify { anyConstructed<TokenManager>().saveLastZombieId("first") }
        coVerify(exactly = 0) { anyConstructed<TokenManager>().saveLastZombieId("second") }
    }

    @Test
    fun doWork_veryLongRunningRecord_handlesLargeDuration() = runBlocking {
        // Given: A record running for 100 hours (extreme case)
        val now = System.currentTimeMillis() / 1000
        val hundredHoursAgo = now - (100 * 3600)
        val record = createTestRecord(key = "longrunner", startTime = hundredHoursAgo)
        val response = TimeTaggerRecordResponse(records = listOf(record))
        
        coEvery { apiService.getRecords(any(), running = 1) } returns response

        // When: Worker executes
        val worker = createWorker()
        val result = worker.doWork()

        // Then: Should return success and handle the large duration
        assertEquals(ListenableWorker.Result.success(), result)
        
        // And: Should send notification
        verify(atLeast = 1) { notificationManager.notify(any(), any()) }
    }

    // Helper function to create a test worker
    private fun createWorker(): ZombieCheckWorker {
        return ZombieCheckWorker(context, workerParams)
    }

    // Helper function to create a test record
    private fun createTestRecord(
        key: String,
        startTime: Long,
        endTime: Long = 0L, // 0 means running
        modifiedTime: Long = System.currentTimeMillis() / 1000,
        description: String = "Test work",
        serverTime: Double = System.currentTimeMillis() / 1000.0
    ): TimeTaggerRecord {
        return TimeTaggerRecord(
            key = key,
            startTime = startTime,
            endTime = endTime,
            modifiedTime = modifiedTime,
            description = description,
            serverTime = serverTime
        )
    }
}
