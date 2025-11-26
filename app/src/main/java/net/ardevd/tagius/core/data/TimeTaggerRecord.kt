package net.ardevd.tagius.core.data

import com.google.gson.annotations.SerializedName

data class TimeTaggerRecord (
    @SerializedName("key")
    val key: String,

    @SerializedName("t1")
    val startTime: Long,
    @SerializedName("t2")
    val endTime: Long,
    @SerializedName("mt")
    val modifiedTime: Long,

    // Description, which may include tags (e.g., "#work")
    @SerializedName("ds")
    val description: String,

    // Server time, which is a floating-point Unix timestamp (seconds since epoch)
    @SerializedName("st")
    val serverTime: Double

)