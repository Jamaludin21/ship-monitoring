package com.example.shipmonitoring.data.model

import com.google.gson.annotations.SerializedName

data class SubmissionResponse(
    val id: String,
    val shipId: String? = null,
    val ship: ShipResponse? = null,
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
    val reviewedAt: String?,
    val reviewedBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val arrivalInspection: ArrivalInspectionResponse? = null
)

data class ShipResponse(
    val id: String,
    val shipNumber: String,
    val name: String,
    val captainId: String? = null,
    val captain: CaptainResponse? = null,
    val locations: List<LocationResponse>? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class CaptainResponse(
    val id: String,
    val name: String,
    val username: String
)

data class LocationResponse(
    val id: String? = null,
    val shipId: String? = null,
    val latitude: Double,
    val longitude: Double,
    val createdAt: String? = null
)

data class RejectSubmissionRequest(
    val reviewNote: String
)

data class ChecklistQuestionResponse(
    val itemNo: Int,
    val question: String
)

data class InspectionItemPayload(
    val itemNo: Int,
    val condition: String,
    val note: String? = null
)

data class ArrivalInspectionItemResponse(
    val id: String? = null,
    val inspectionId: String? = null,
    val itemNo: Int,
    val question: String,
    val condition: String,
    val note: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class ArrivalInspectionResponse(
    val id: String,
    val submissionId: String,
    val inspectionDocumentUrl: String? = null,
    val responseLetterUrl: String? = null,
    val note: String? = null,
    val checkedBy: String,
    val checkedAt: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val items: List<ArrivalInspectionItemResponse> = emptyList(),
    val submission: SubmissionResponse? = null
)

data class ShipSummaryResponse(
    val id: String,
    val shipNumber: String,
    val name: String,
    val captain: CaptainResponse? = null,
    val latestLocation: LocationResponse? = null,
    val latestSubmission: SubmissionResponse? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class ShipHistoryShipResponse(
    val id: String,
    val shipNumber: String,
    val name: String,
    val captain: CaptainResponse? = null
)

data class ShipHistoryResponse(
    val message: String? = null,
    val data: List<SubmissionResponse>? = null,
    val ship: ShipHistoryShipResponse? = null
)

data class HealthResponse(
    @SerializedName("status")
    val status: String? = null
)
