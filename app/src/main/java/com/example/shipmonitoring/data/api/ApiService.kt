package com.example.shipmonitoring.data.api

import com.example.shipmonitoring.data.model.LoginRequest
import com.example.shipmonitoring.data.model.LoginResponse
import com.example.shipmonitoring.data.model.UpdateLocationRequest
import com.example.shipmonitoring.data.model.ShipLocation
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @Multipart
    @POST("upload/document")
    suspend fun uploadDocument(
        @Part("shipId") shipId: RequestBody,
        @Part document: MultipartBody.Part
    ): Response<Any>

    @POST("location/update")
    suspend fun updateLocation(@Body request: UpdateLocationRequest): Response<Any>

    @GET("location/ships")
    suspend fun getAllShipLocations(): Response<List<ShipLocation>>
}