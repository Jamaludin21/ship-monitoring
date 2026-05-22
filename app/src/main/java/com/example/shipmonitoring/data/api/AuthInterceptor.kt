package com.example.shipmonitoring.data.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        val baseRequest = chain.request()

        val request = if (!token.isNullOrBlank()) {
            baseRequest
                .newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            baseRequest
        }

        return chain.proceed(request)
    }
}
