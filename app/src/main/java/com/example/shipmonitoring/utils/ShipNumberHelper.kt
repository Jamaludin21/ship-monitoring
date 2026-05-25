package com.example.shipmonitoring.utils

private val kmPrefixRegex = Regex("^\\s*KM\\s*-?\\s*", RegexOption.IGNORE_CASE)
private val nonDigitRegex = Regex("[^0-9]")

fun sanitizeShipNumberInput(raw: String): String {
    return raw
        .replace(kmPrefixRegex, "")
        .replace(nonDigitRegex, "")
}

fun withKmPrefix(raw: String): String {
    val numberPart = sanitizeShipNumberInput(raw)
    return if (numberPart.isBlank()) "" else "KM-$numberPart"
}
