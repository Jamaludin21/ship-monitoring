package com.example.shipmonitoring.data.api

import com.example.shipmonitoring.data.model.ApiEnvelope
import com.example.shipmonitoring.data.model.BaseResponse
import com.example.shipmonitoring.data.model.LoginRequest
import com.example.shipmonitoring.data.model.LoginResponse
import com.example.shipmonitoring.data.model.RejectSubmissionRequest
import com.example.shipmonitoring.data.model.SubmissionResponse
import com.example.shipmonitoring.data.model.UpdateLocationRequest
import com.example.shipmonitoring.data.model.ShipLocation
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("location/update")
    suspend fun updateLocation(@Body request: UpdateLocationRequest): Response<BaseResponse>

    @GET("location/ships")
    suspend fun getAllShipLocations(): Response<ApiEnvelope<List<ShipLocation>>>

    @Multipart
    @POST("submissions")
    suspend fun createSubmission(
        @Part("captainName") captainName: RequestBody,
        @Part("employeeCount") employeeCount: RequestBody,
        @Part("cargo") cargo: RequestBody,
        @Part("cargoAmount") cargoAmount: RequestBody,
        @Part sailingPermit: MultipartBody.Part,
        @Part callSignCertificate: MultipartBody.Part,
        @Part safetyCertificate: MultipartBody.Part,
        @Part radioStationPermit: MultipartBody.Part
    ): Response<BaseResponse>

    @GET("submissions")
    suspend fun getSubmissions(): Response<ApiEnvelope<List<SubmissionResponse>>>

    @GET("submissions/my-history")
    suspend fun getMySubmissionHistory(): Response<ApiEnvelope<List<SubmissionResponse>>>

    @GET("submissions/ship/{shipNumber}/history")
    suspend fun getShipHistory(
        @Path("shipNumber") shipNumber: String
    ): Response<ApiEnvelope<List<SubmissionResponse>>>

    @PATCH("submissions/{id}/approve")
    suspend fun approveSubmission(
        @Path("id") id: String
    ): Response<BaseResponse>

    @PATCH("submissions/{id}/reject")
    suspend fun rejectSubmission(
        @Path("id") id: String,
        @Body request: RejectSubmissionRequest
    ): Response<BaseResponse>
}
