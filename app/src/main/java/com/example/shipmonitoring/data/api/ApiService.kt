package com.example.shipmonitoring.data.api

import com.example.shipmonitoring.data.model.ApiEnvelope
import com.example.shipmonitoring.data.model.BaseResponse
import com.example.shipmonitoring.data.model.ChecklistQuestionResponse
import com.example.shipmonitoring.data.model.HealthResponse
import com.example.shipmonitoring.data.model.LoginRequest
import com.example.shipmonitoring.data.model.LoginResponse
import com.example.shipmonitoring.data.model.RejectSubmissionRequest
import com.example.shipmonitoring.data.model.ArrivalInspectionResponse
import com.example.shipmonitoring.data.model.ShipSummaryResponse
import com.example.shipmonitoring.data.model.SubmissionResponse
import com.example.shipmonitoring.data.model.ShipHistoryResponse
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
import retrofit2.http.PUT
import retrofit2.http.Query

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("health")
    suspend fun health(): Response<HealthResponse>

    @GET("ships")
    suspend fun getShips(): Response<ApiEnvelope<List<ShipSummaryResponse>>>

    @GET("ships/my")
    suspend fun getMyShips(): Response<ApiEnvelope<List<ShipSummaryResponse>>>

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
    ): Response<ApiEnvelope<SubmissionResponse>>

    @GET("submissions")
    suspend fun getSubmissions(
        @Query("status") status: String? = null,
        @Query("shipNumber") shipNumber: String? = null
    ): Response<ApiEnvelope<List<SubmissionResponse>>>

    @GET("submissions/my-history")
    suspend fun getMySubmissionHistory(): Response<ApiEnvelope<List<SubmissionResponse>>>

    @GET("submissions/{id}")
    suspend fun getSubmissionDetail(
        @Path("id") id: String
    ): Response<ApiEnvelope<SubmissionResponse>>

    @GET("submissions/ship/{shipNumber}/history")
    suspend fun getShipHistory(
        @Path("shipNumber") shipNumber: String
    ): Response<ShipHistoryResponse>

    @PATCH("submissions/{id}/approve")
    suspend fun approveSubmission(
        @Path("id") id: String
    ): Response<ApiEnvelope<SubmissionResponse>>

    @PATCH("submissions/{id}/reject")
    suspend fun rejectSubmission(
        @Path("id") id: String,
        @Body request: RejectSubmissionRequest
    ): Response<ApiEnvelope<SubmissionResponse>>

    @GET("submissions/arrival-inspection/checklist")
    suspend fun getArrivalInspectionChecklist(): Response<ApiEnvelope<List<ChecklistQuestionResponse>>>

    @GET("submissions/{id}/arrival-inspection")
    suspend fun getArrivalInspection(
        @Path("id") id: String
    ): Response<ApiEnvelope<ArrivalInspectionResponse>>

    @Multipart
    @PUT("submissions/{id}/arrival-inspection")
    suspend fun upsertArrivalInspection(
        @Path("id") id: String,
        @Part("inspectionItems") inspectionItems: RequestBody? = null,
        @Part("note") note: RequestBody? = null,
        @Part inspectionDocument: MultipartBody.Part? = null,
        @Part responseLetter: MultipartBody.Part? = null
    ): Response<ApiEnvelope<ArrivalInspectionResponse>>
}
