package com.example.shipmonitoring.data.api

import android.content.Context
import com.example.shipmonitoring.data.session.SessionManager

object AppContainer {
    private var initialized = false

    lateinit var sessionManager: SessionManager
        private set

    fun init(context: Context) {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            sessionManager = SessionManager(context.applicationContext)
            RetrofitClient.init {
                sessionManager.currentToken()
            }

            initialized = true
        }
    }

    val apiService: ApiService
        get() {
            check(initialized) {
                "AppContainer belum diinisialisasi. Panggil AppContainer.init(context) di awal aplikasi."
            }
            return RetrofitClient.apiService
        }
}
