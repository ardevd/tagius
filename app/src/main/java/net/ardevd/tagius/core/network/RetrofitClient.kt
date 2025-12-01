package net.ardevd.tagius.core.network

import android.content.Context
import net.ardevd.tagius.core.data.TokenManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {


    // Volatile ensures atomic access for thread safety
    @Volatile
    private var apiServiceInstance: TimeTaggerApiService? = null

    fun getInstance(context: Context): TimeTaggerApiService {
        return apiServiceInstance ?: synchronized(this) {
            val instance = buildApiService(context)
            apiServiceInstance = instance
            instance
        }
    }

    // TODO: Call this when adding support for switching servers or user log-out
    fun reset() {
        apiServiceInstance = null
    }

    private fun buildApiService(context: Context): TimeTaggerApiService {

        val tokenManager = TokenManager(context)
        val authInterceptor = AuthInterceptor(tokenManager)

        val baseUrl = tokenManager.getServerUrlBlocking()


        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()


        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(TimeTaggerApiService::class.java)
    }
}