package id.calltag.app.data.remote

import id.calltag.app.data.remote.dto.ApiResponseDto
import id.calltag.app.data.remote.dto.GoogleLoginRequestDto
import id.calltag.app.data.remote.dto.AuthLoginResponseDto
import id.calltag.app.data.remote.dto.DisputeRequestDto
import id.calltag.app.data.remote.dto.DisputeResponseDto
import id.calltag.app.data.remote.dto.EmergencyResponseDto
import id.calltag.app.data.remote.dto.LookupDataDto
import id.calltag.app.data.remote.dto.MeResponseDto
import id.calltag.app.data.remote.dto.OfficialChannelDto
import id.calltag.app.data.remote.dto.PhoneReportsDataDto
import id.calltag.app.data.remote.dto.PublicStatsDto
import id.calltag.app.data.remote.dto.RefreshRequestDto
import id.calltag.app.data.remote.dto.ReportRequestDto
import id.calltag.app.data.remote.dto.ReportResponseDto
import id.calltag.app.data.remote.dto.SpamSyncResponseDto
import id.calltag.app.data.remote.dto.SyncChangesResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Kontrak resmi Retrofit API (`ApiService.kt`) yang 100% tersinkronisasi
 * dengan Bagian 5.3 `CallTag/PRD.md`:
 * - POST /api/v1/auth/google
 * - GET  /api/v1/phones/{e164}
 * - GET  /api/v1/phones/{e164}/reports
 * - POST /api/v1/reports
 * - POST /api/v1/disputes
 * - GET  /api/v1/emergency/national
 * - GET  /api/v1/emergency/local
 * - GET  /api/v1/official-channels
 * - GET  /api/v1/sync/offline-bundle
 * - GET  /api/v1/stats/public
 */
interface ApiService {

    @GET("health")
    suspend fun getHealth(): Response<okhttp3.ResponseBody>

    @POST("api/v1/auth/google")
    suspend fun authenticateGoogle(
        @Body request: GoogleLoginRequestDto
    ): ApiResponseDto<AuthLoginResponseDto>

    @POST("api/v1/auth/refresh")
    suspend fun refreshAuthToken(
        @Header("X-Device-Id") deviceId: String? = null,
        @Body request: RefreshRequestDto
    ): ApiResponseDto<AuthLoginResponseDto>

    @GET("api/v1/me")
    suspend fun getMe(
        @Header("Authorization") bearerToken: String? = null,
        @Header("X-Device-Id") deviceId: String? = null
    ): ApiResponseDto<MeResponseDto>

    @DELETE("api/v1/me")
    suspend fun deleteMe(
        @Header("Authorization") bearerToken: String? = null,
        @Header("X-Device-Id") deviceId: String? = null
    ): Response<ApiResponseDto<Map<String, Any>>>

    @GET("api/v1/phones/{e164}")
    suspend fun getPhoneDetails(
        @Path(value = "e164", encoded = true) e164: String
    ): ApiResponseDto<LookupDataDto>

    @GET("api/v1/phones/{e164}/reports")
    suspend fun getPhoneReports(
        @Path(value = "e164", encoded = true) e164: String
    ): ApiResponseDto<PhoneReportsDataDto>

    @POST("api/v1/reports")
    suspend fun submitReport(
        @Header("Authorization") bearerToken: String? = null,
        @Header("X-Device-Id") deviceId: String? = null,
        @Header("Idempotency-Key") idempotencyKey: String? = null,
        @Body request: ReportRequestDto
    ): ApiResponseDto<ReportResponseDto>

    @POST("api/v1/disputes")
    suspend fun submitDispute(
        @Header("Authorization") bearerToken: String? = null,
        @Header("X-Device-Id") deviceId: String? = null,
        @Body request: DisputeRequestDto
    ): ApiResponseDto<DisputeResponseDto>

    @GET("api/v1/disputes/{id}")
    suspend fun getDisputeStatus(
        @Path("id") disputeId: String,
        @Header("X-Tracking-Token") trackingToken: String? = null,
        @Header("Authorization") bearerToken: String? = null
    ): ApiResponseDto<DisputeResponseDto>

    @GET("api/v1/emergency/national")
    suspend fun getNationalEmergencyDirectory(
        @Query("type") agencyType: String? = null
    ): ApiResponseDto<EmergencyResponseDto>

    @GET("api/v1/emergency/local")
    suspend fun getLocalEmergencyDirectory(
        @Query("city") city: String? = null,
        @Query("province") province: String? = null,
        @Query("type") agencyType: String? = null
    ): ApiResponseDto<EmergencyResponseDto>

    @GET("api/v1/official-channels")
    suspend fun getOfficialChannels(
        @Query("category") category: String? = null
    ): ApiResponseDto<List<OfficialChannelDto>>

    @GET("api/v1/sync/offline-bundle")
    suspend fun syncOfflineBundle(
        @Query("since_version") sinceVersion: Long = 0L,
        @Query("limit") limit: Int = 200
    ): ApiResponseDto<SpamSyncResponseDto>

    @GET("api/v1/sync/changes")
    suspend fun getSyncChanges(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 200
    ): Response<ApiResponseDto<SyncChangesResponseDto>>

    @GET("api/v1/stats/public")
    suspend fun getPublicStats(): ApiResponseDto<PublicStatsDto>
}
