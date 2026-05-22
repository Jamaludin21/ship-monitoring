package com.example.shipmonitoring.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(val username: String, val password: String)

data class UserData(
    val id: String,
    val name: String,
    val role: String,
    val shipId: String? = null,
    val shipNumber: String? = null,
    val shipName: String? = null,
    @SerializedName("ship")
    val ship: ShipResponse? = null
) {
    val resolvedShipId: String?
        get() = shipId ?: ship?.id

    val resolvedShipNumber: String?
        get() = shipNumber ?: ship?.shipNumber

    val resolvedShipName: String?
        get() = shipName ?: ship?.name
}

data class LoginResponse(
    val message: String,
    val token: String?,
    val data: UserData?
)
