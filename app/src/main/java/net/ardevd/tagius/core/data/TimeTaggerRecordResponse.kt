package net.ardevd.tagius.core.data

import com.google.gson.annotations.SerializedName

data class TimeTaggerRecordResponse(
    @SerializedName("records")
    val records: List<TimeTaggerRecord>
)
