package com.example.shipmonitoring.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

fun getFileFromUri(context: Context, uri: Uri): File? {
    val contentResolver = context.contentResolver

    // Ambil nama asli file
    var fileName = "temp_document_${System.currentTimeMillis()}"
    val cursor = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = it.getString(nameIndex)
            }
        }
    }

    val sanitizedFileName = fileName
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .take(100)
        .ifBlank { "temp_document_${System.currentTimeMillis()}" }

    // Buat file sementara di folder cache aplikasi
    val tempFile = File(context.cacheDir, sanitizedFileName)

    return try {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(tempFile)

        inputStream?.copyTo(outputStream)

        inputStream?.close()
        outputStream.close()
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getFileSizeFromUri(context: Context, uri: Uri): Long? {
    val projection = arrayOf(OpenableColumns.SIZE)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (sizeIndex != -1 && cursor.moveToFirst()) {
            return cursor.getLong(sizeIndex)
        }
    }
    return null
}

fun getDisplayNameFromUri(context: Context, uri: Uri): String? {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
            return cursor.getString(nameIndex)
        }
    }
    return null
}

fun isPdfUri(context: Context, uri: Uri): Boolean {
    val mimeType = context.contentResolver.getType(uri)?.lowercase(Locale.US)
    if (mimeType == "application/pdf") {
        return true
    }

    val displayName = getDisplayNameFromUri(context, uri)?.lowercase(Locale.US)
    return displayName?.endsWith(".pdf") == true
}
