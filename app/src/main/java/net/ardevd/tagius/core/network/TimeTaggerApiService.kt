package net.ardevd.tagius.core.network

import net.ardevd.tagius.core.data.TimeTaggerPutResponse
import net.ardevd.tagius.core.data.TimeTaggerRecord
import net.ardevd.tagius.core.data.TimeTaggerRecordResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface TimeTaggerApiService {


    @PUT("records")
    suspend fun updateRecords(@Body records: List<TimeTaggerRecord>): TimeTaggerPutResponse

    // We use the settings endpoint as a "ping" endpoint to verify login credentials
    @GET("settings")
    suspend fun getSettings(): Any

    @GET("records")
    suspend fun getRecords(
        @Query("timerange") timerange: String,
        @Query("running") running: Int? = null,
        @Query("hidden") hidden: Int? = 0,
        @Query("tag") tag: String? = null
    ): TimeTaggerRecordResponse
}