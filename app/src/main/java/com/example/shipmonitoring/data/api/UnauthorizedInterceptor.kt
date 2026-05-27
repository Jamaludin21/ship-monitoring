package com.example.shipmonitoring.data.api

import okhttp3.Interceptor
import okhttp3.Response

class UnauthorizedInterceptor(
    private val onUnauthorized: () -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val hasBearerToken = request.header("Authorization")?.startsWith("Bearer ") == true
        val response = chain.proceed(request)
        if (response.code == 401 && hasBearerToken) {
            onUnauthorized()
        }
        return response
    }
}
