package com.example.shipmonitoring.data.model

data class SubmissionResponse(
    val id: String,
    val ship: ShipResponse,
    val captainName: String,
    val employeeCount: Int,
    val cargo: String,
    val cargoAmount: String,
    val sailingPermitUrl: String,
    val callSignCertificateUrl: String,
    val safetyCertificateUrl: String,
    val radioStationPermitUrl: String,
    val status: String,
    val reviewNote: String?,
    val submittedAt: String,
    val reviewedAt: String?
)

data class ShipResponse(
    val id: String,
    val shipNumber: String,
    val name: String
)

data class RejectSubmissionRequest(
    val reviewNote: String
)
