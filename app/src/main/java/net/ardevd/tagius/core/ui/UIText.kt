package net.ardevd.tagius.core.ui

import android.content.Context
import androidx.annotation.StringRes

sealed class UIText {
    data class DynamicString(val value: String) : UIText()

    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any // Allows for format arguments like "Error: %d"
    ) : UIText()

    // Helper to resolve the string in the UI
    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId, *args)
        }
    }
}