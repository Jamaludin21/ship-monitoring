package com.example.shipmonitoring.data.model

import com.google.gson.annotations.SerializedName

data class UpdateLocationRequest(
    val latitude: Double,
    val longitude: Double
)

data class ShipLocation(
    val shipId: String,
    val shipNumber: String,
    val shipName: String,
    val captain: CaptainResponse? = null,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
    val latestSubmission: SubmissionResponse? = null
) {
    val lastUpdatedAt: String?
        get() = updatedAt
}
