package net.ardevd.tagius.core.data

data class TimeTaggerPutResponse(
    val accepted: List<String>, // Keys of successfully updated records
    val failed: List<String>,   // Keys of rejected records
    val errors: Map<String, String>? // Error messages (optional)
)