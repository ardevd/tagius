package net.ardevd.tagius.core.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object LoginRetrofitClient {

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

        val baseAPIURI = if (baseUrl.startsWith("https://timetagger.app")) "api/v2/" else "timetagger/api/v2/"

        return Retrofit.Builder()
            .baseUrl(validUrl + baseAPIURI)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TimeTaggerApiService::class.java)
    }
}