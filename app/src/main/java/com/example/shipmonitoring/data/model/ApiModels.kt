package com.example.shipmonitoring.data.model

data class ApiEnvelope<T>(
    val message: String? = null,
    val data: T? = null
)

typealias BaseResponse = ApiEnvelope<Any?>
