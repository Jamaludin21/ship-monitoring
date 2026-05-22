package com.example.shipmonitoring.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.sessionDataStore by preferencesDataStore(name = "session")

data class UserSession(
    val token: String,
    val role: String,
    val userName: String,
    val userId: String?,
    val shipId: String?,
    val shipNumber: String?,
    val shipName: String?
)

class SessionManager(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedToken: String? = null

    companion object {
        val TOKEN_KEY = stringPreferencesKey("token")
        val ROLE_KEY = stringPreferencesKey("role")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_ID_KEY = stringPreferencesKey("user_id")
        val SHIP_ID_KEY = stringPreferencesKey("ship_id")
        val SHIP_NUMBER_KEY = stringPreferencesKey("ship_number")
        val SHIP_NAME_KEY = stringPreferencesKey("ship_name")
    }

    val tokenFlow: Flow<String?> = appContext.sessionDataStore.data.map { prefs ->
        prefs[TOKEN_KEY]
    }

    val sessionFlow: Flow<UserSession?> = appContext.sessionDataStore.data.map { prefs ->
        val token = prefs[TOKEN_KEY]
        val role = prefs[ROLE_KEY]
        val name = prefs[USER_NAME_KEY]

        if (token.isNullOrBlank() || role.isNullOrBlank() || name.isNullOrBlank()) {
            null
        } else {
            UserSession(
                token = token,
                role = role,
                userName = name,
                userId = prefs[USER_ID_KEY],
                shipId = prefs[SHIP_ID_KEY],
                shipNumber = prefs[SHIP_NUMBER_KEY],
                shipName = prefs[SHIP_NAME_KEY]
            )
        }
    }

    init {
        scope.launch {
            tokenFlow.collectLatest { token ->
                cachedToken = token
            }
        }
    }

    suspend fun saveSession(
        token: String,
        role: String,
        name: String,
        userId: String?,
        shipId: String?,
        shipNumber: String?,
        shipName: String?
    ) {
        appContext.sessionDataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[ROLE_KEY] = role
            prefs[USER_NAME_KEY] = name

            if (userId.isNullOrBlank()) {
                prefs.remove(USER_ID_KEY)
            } else {
                prefs[USER_ID_KEY] = userId
            }

            if (shipId.isNullOrBlank()) {
                prefs.remove(SHIP_ID_KEY)
            } else {
                prefs[SHIP_ID_KEY] = shipId
            }

            if (shipNumber.isNullOrBlank()) {
                prefs.remove(SHIP_NUMBER_KEY)
            } else {
                prefs[SHIP_NUMBER_KEY] = shipNumber
            }

            if (shipName.isNullOrBlank()) {
                prefs.remove(SHIP_NAME_KEY)
            } else {
                prefs[SHIP_NAME_KEY] = shipName
            }
        }
    }

    fun currentToken(): String? = cachedToken

    suspend fun clearSession() {
        appContext.sessionDataStore.edit { it.clear() }
    }
}
