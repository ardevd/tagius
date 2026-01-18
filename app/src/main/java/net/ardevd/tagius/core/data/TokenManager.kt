package net.ardevd.tagius.core.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {

    companion object {
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_LAST_DESCRIPTION = stringPreferencesKey("last_description")
        private const val DEFAULT_URL = "https://timetagger.app/"
    }

    // Read the last description (default to empty string)
    val lastDescriptionFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_LAST_DESCRIPTION] ?: ""
        }

    suspend fun saveLastDescription(description: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_DESCRIPTION] = description
        }
    }

    val authTokenFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_AUTH_TOKEN]
        }

    val serverUrlFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SERVER_URL] ?: DEFAULT_URL
        }



    suspend fun saveConnectionDetails(url: String, token: String) {
        val cleanUrl = if (url.endsWith("/")) url else "$url/"

        context.dataStore.edit { preferences ->
            preferences[KEY_SERVER_URL] = cleanUrl
            preferences[KEY_AUTH_TOKEN] = token
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_AUTH_TOKEN)
            preferences.remove(KEY_LAST_DESCRIPTION)
        }
    }

    fun getTokenBlocking(): String? = runBlocking {
        authTokenFlow.first()
    }

    fun getServerUrlBlocking(): String = runBlocking {
        serverUrlFlow.first() + "timetagger/api/v2/"
    }



}