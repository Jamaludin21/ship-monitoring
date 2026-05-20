package com.example.shipmonitoring.data.model

data class LoginRequest(val username: String, val password: String)

data class UserData(val id: String, val name: String, val role: String, val shipId: String? = null)

data class LoginResponse(
    val message: String,
    val token: String?,
    val data: UserData?
)