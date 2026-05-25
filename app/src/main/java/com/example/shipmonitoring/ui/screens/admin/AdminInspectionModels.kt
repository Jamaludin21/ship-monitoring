package com.example.shipmonitoring.ui.screens.admin

enum class ChecklistChoice {
    YES,
    NO,
    UNSET
}

data class InspectionChecklistItem(
    val number: Int,
    val question: String,
    val choice: ChecklistChoice = ChecklistChoice.UNSET,
    val note: String = ""
)

data class SubmissionInspectionData(
    val useChecklistForm: Boolean = true,
    val checklistItems: List<InspectionChecklistItem> = defaultInspectionChecklistItems(),
    val inspectionDocName: String? = null,
    val inspectionDocUri: String? = null,
    val replyLetterDocName: String? = null,
    val replyLetterDocUri: String? = null,
    val summaryNote: String = "",
    val updatedAtMillis: Long? = null
)

fun defaultInspectionChecklistItems(): List<InspectionChecklistItem> = listOf(
    InspectionChecklistItem(1, "Radar berfungsi dengan baik"),
    InspectionChecklistItem(2, "Echosounder berfungsi dengan baik"),
    InspectionChecklistItem(3, "Lampu-lampu navigasi berfungsi dengan baik"),
    InspectionChecklistItem(4, "GPS Navigator berfungsi dengan baik"),
    InspectionChecklistItem(5, "AIS berfungsi dengan baik"),
    InspectionChecklistItem(7, "VHF Radio berfungsi dengan baik"),
    InspectionChecklistItem(8, "Rudder Indicator berfungsi dengan baik"),
    InspectionChecklistItem(9, "Magnetic Compass berfungsi dengan baik"),
    InspectionChecklistItem(10, "Radio HT berfungsi dengan baik"),
    InspectionChecklistItem(11, "Joy stick dan kemudi berfungsi dengan baik"),
    InspectionChecklistItem(12, "Radio SSB berfungsi dengan baik"),
    InspectionChecklistItem(13, "ECDIS NE430 SAMYUNG"),
    InspectionChecklistItem(14, "NAVTEX")
)
