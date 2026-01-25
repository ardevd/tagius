package net.ardevd.tagius.core.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit

object LoginRetrofitClient {

    /**
     * Determines the correct API path based on the server URL.
     * 
     * @param baseUrl The base URL of the TimeTagger server
     * @return "api/v2/" for hosted timetagger.app, "timetagger/api/v2/" for self-hosted instances
     */
    internal fun determineApiPath(baseUrl: String): String {
        return try {
            val url = URL(baseUrl.lowercase())
            // If host is null, default to empty string which will trigger self-hosted path
            val host = url.host?.removePrefix("www.") ?: ""
            if (host == "timetagger.app") "api/v2/" else "timetagger/api/v2/"
        } catch (e: MalformedURLException) {
            // If URL parsing fails, default to self-hosted path
            "timetagger/api/v2/"
        }
    }

    fun createTemporaryService(baseUrl: String, token: String): TimeTaggerApiService {
        // Ensure URL ends with /
        val validUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val authInterceptor = Interceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .addHeader("authtoken", token)
                .build()
            chain.proceed(newRequest)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()

        val baseAPIURI = determineApiPath(baseUrl)

        return Retrofit.Builder()
            .baseUrl(validUrl + baseAPIURI)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TimeTaggerApiService::class.java)
    }
}