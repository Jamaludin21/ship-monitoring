package com.example.shipmonitoring.data.api

import com.example.shipmonitoring.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private var tokenProvider: (() -> String?)? = null
    private var unauthorizedHandler: (() -> Unit)? = null
    private var retrofit: Retrofit? = null
    private var service: ApiService? = null

    fun init(
        tokenProvider: () -> String?,
        unauthorizedHandler: () -> Unit
    ) {
        this.tokenProvider = tokenProvider
        this.unauthorizedHandler = unauthorizedHandler
        retrofit = null
        service = null
    }

    private fun loggingLevel(): HttpLoggingInterceptor.Level {
        return when (BuildConfig.HTTP_LOG_LEVEL.uppercase()) {
            "BODY" -> HttpLoggingInterceptor.Level.BODY
            "HEADERS" -> HttpLoggingInterceptor.Level.HEADERS
            "BASIC" -> HttpLoggingInterceptor.Level.BASIC
            else -> HttpLoggingInterceptor.Level.NONE
        }
    }

    private fun normalizedBaseUrl(rawUrl: String): String {
        return if (rawUrl.endsWith("/")) rawUrl else "$rawUrl/"
    }

    private fun buildClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = loggingLevel()
        }

        val authInterceptor = AuthInterceptor {
            tokenProvider?.invoke()
        }
        val unauthorizedInterceptor = UnauthorizedInterceptor {
            unauthorizedHandler?.invoke()
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(unauthorizedInterceptor)
            .addInterceptor(logging)
            .build()
    }

    private fun buildService(): ApiService {
        val instance = Retrofit.Builder()
            .baseUrl(normalizedBaseUrl(BuildConfig.API_BASE_URL))
            .client(buildClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit = instance
        return instance.create(ApiService::class.java)
    }

    val apiService: ApiService
        get() {
            service?.let { return it }
            val created = buildService()
            service = created
            return created
        }
}
