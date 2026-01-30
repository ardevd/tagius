package net.ardevd.tagius.core.data

import com.google.gson.annotations.SerializedName

data class TimeTaggerPutResponse(
    @SerializedName("accepted")
    val accepted: List<String>, // Keys of successfully updated record
    @SerializedName("failed")
    val failed: List<String>,   // Keys of rejected records
    @SerializedName("errors")
    val errors: Map<String, String>? // Error messages (optional)
)