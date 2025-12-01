package net.ardevd.tagius.core.network

import net.ardevd.tagius.core.data.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val token = tokenManager.getTokenBlocking()

        val newRequestBuilder = originalRequest.newBuilder()

        if (!token.isNullOrBlank()) {
            newRequestBuilder.addHeader("authtoken", token)
        }

        return chain.proceed(newRequestBuilder.build())
    }
}