package com.example.shipmonitoring.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

private val apiDateFormats = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSSX",
    "yyyy-MM-dd'T'HH:mm:ssX",
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ss'Z'"
)

private fun parseIsoDate(value: String?): Date? {
    if (value.isNullOrBlank()) return null

    for (pattern in apiDateFormats) {
        val parser = SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }

        try {
            return parser.parse(value)
        } catch (_: Exception) {
            // Try next format.
        }
    }

    return null
}

fun formatDateShort(value: String?): String {
    val date = parseIsoDate(value) ?: return "-"
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
    return formatter.format(date)
}

fun formatDateTime(value: String?): String {
    val date = parseIsoDate(value) ?: return "-"
    val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
    return formatter.format(date)
}

fun statusLabel(status: String): String {
    return when (status.uppercase()) {
        "PENDING" -> "Menunggu Validasi"
        "APPROVED" -> "Disetujui"
        "REJECTED" -> "Ditolak"
        else -> status
    }
}

fun isLocationActive(lastUpdatedAt: String?, staleMinutes: Long = 5L): Boolean {
    val parsedDate = parseIsoDate(lastUpdatedAt) ?: return false
    val diffMillis = abs(System.currentTimeMillis() - parsedDate.time)
    return diffMillis <= staleMinutes * 60L * 1000L
}

fun relativeTimeLabel(lastUpdatedAt: String?): String {
    val parsedDate = parseIsoDate(lastUpdatedAt) ?: return "Tidak diketahui"
    val diffMillis = System.currentTimeMillis() - parsedDate.time
    if (diffMillis < 0L) return "Baru saja"

    val totalMinutes = diffMillis / (60L * 1000L)
    if (totalMinutes < 1L) return "Baru saja"
    if (totalMinutes < 60L) return "$totalMinutes menit lalu"

    val totalHours = totalMinutes / 60L
    if (totalHours < 24L) return "$totalHours jam lalu"

    val totalDays = totalHours / 24L
    return "$totalDays hari lalu"
}
