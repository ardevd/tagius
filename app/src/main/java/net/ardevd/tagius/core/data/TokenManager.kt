package net.ardevd.tagius.core.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class TokenManager(context: Context) {

    // In production, use EncryptedSharedPreferences
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_SERVER_URL = "server_url"
        private const val DEFAULT_URL = "https://timetagger.app/"
    }

    fun saveConnectionDetails(url: String, token: String) {
        // Ensure URL ends with / to avoid Retrofit crashes
        val cleanUrl = if (url.endsWith("/")) url else "$url/"

        prefs.edit {
            putString(KEY_SERVER_URL, cleanUrl)
                .putString(KEY_AUTH_TOKEN, token)
        }
    }

    fun hasSession(): Boolean {
        return !getToken().isNullOrBlank()
    }

    fun getServerUrl(): String {
        val baseUrl =  prefs.getString(KEY_SERVER_URL, DEFAULT_URL) ?: DEFAULT_URL
        return baseUrl + "timetagger/api/v2/"
    }

    fun getToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, "")
    }
}