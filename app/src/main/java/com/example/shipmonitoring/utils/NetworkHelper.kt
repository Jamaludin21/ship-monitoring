package com.example.shipmonitoring.utils

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.json.JSONObject
import retrofit2.Response

fun <T> extractErrorMessage(response: Response<T>, fallback: String): String {
    val code = response.code()
    val errorText = try {
        response.errorBody()?.string()
    } catch (_: Exception) {
        null
    }

    if (!errorText.isNullOrBlank()) {
        try {
            val json = JSONObject(errorText)
            val message = json.optString("message")
            if (message.isNotBlank()) {
                return message
            }
        } catch (_: Exception) {
            // Ignore invalid JSON and fallback to default message.
        }
    }

    val defaultMessage = when (code) {
        400 -> "Permintaan tidak valid."
        401 -> "Sesi login berakhir. Silakan login ulang."
        403 -> "Akses ditolak untuk fitur ini."
        404 -> "Data tidak ditemukan."
        409 -> "Terjadi konflik data. Silakan periksa input Anda."
        else -> null
    }

    return if (defaultMessage != null) {
        "$defaultMessage (HTTP $code)"
    } else {
        "$fallback (HTTP $code)"
    }
}

fun toUserFriendlyNetworkMessage(error: Throwable): String {
    val rawMessage = error.localizedMessage.orEmpty()

    return when {
        error is UnknownHostException -> "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."
        error is SocketTimeoutException -> "Koneksi ke server timeout. Silakan coba lagi."
        rawMessage.contains("Expected BEGIN_ARRAY", ignoreCase = true) ||
            rawMessage.contains("Expected BEGIN_OBJECT", ignoreCase = true) -> {
            "Format data dari server tidak sesuai. Silakan coba lagi."
        }
        else -> "Terjadi kesalahan jaringan. Silakan coba lagi."
    }
}
