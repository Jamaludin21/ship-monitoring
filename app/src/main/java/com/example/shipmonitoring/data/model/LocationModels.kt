package com.example.shipmonitoring.data.model

data class UpdateLocationRequest(
    val shipId: String,
    val latitude: Double,
    val longitude: Double
)

data class ShipLocation(
    val shipId: String,
    val shipNumber: String,
    val shipName: String,
    val latitude: Double,
    val longitude: Double,
    val lastUpdatedAt: String?
)
