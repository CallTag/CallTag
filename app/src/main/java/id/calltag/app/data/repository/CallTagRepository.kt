package id.calltag.app.data.repository

import id.calltag.app.BuildConfig
import id.calltag.app.data.local.OfflineSeedData
import id.calltag.app.data.local.dao.CallScreeningLogDao
import id.calltag.app.data.local.dao.EmergencyContactDao
import id.calltag.app.data.local.dao.PendingDisputeDao
import id.calltag.app.data.local.dao.PendingReportDao
import id.calltag.app.data.local.dao.PhoneTagDao
import id.calltag.app.data.local.dao.SyncStateDao
import id.calltag.app.data.local.entity.CachedPhoneTag
import id.calltag.app.data.local.entity.CallScreeningLog
import id.calltag.app.data.local.entity.EmergencyContact
import id.calltag.app.data.local.entity.PendingDisputeEntity
import id.calltag.app.data.local.entity.PendingReportEntity
import id.calltag.app.data.local.entity.SyncStateEntity
import id.calltag.app.data.preferences.CallTagPreferences
import id.calltag.app.data.preferences.SessionAccessPolicy
import id.calltag.app.data.remote.ApiService
import id.calltag.app.data.remote.dto.GoogleLoginRequestDto
import id.calltag.app.data.remote.dto.DisputeRequestDto
import id.calltag.app.data.remote.dto.DisputeResponseDto
import id.calltag.app.data.remote.dto.EmergencyDirectoryDto
import id.calltag.app.data.remote.dto.LookupDataDto
import id.calltag.app.data.remote.dto.MeResponseDto
import id.calltag.app.data.remote.dto.OfficialChannelDto
import id.calltag.app.data.remote.dto.PhoneTagDto
import id.calltag.app.data.remote.dto.ReportRequestDto
import id.calltag.app.data.remote.dto.ReportResponseDto
import id.calltag.app.data.remote.dto.ReputationLevels
import id.calltag.app.data.remote.dto.ScamCategories
import id.calltag.app.data.remote.dto.SpamReportItemDto
import id.calltag.app.util.PhoneNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException
import java.util.UUID
import kotlin.system.measureNanoTime

enum class LookupAvailability {
    AVAILABLE,
    NOT_FOUND,
    UNAVAILABLE
}

enum class LookupDisplayStatus {
    DANGER_SCAM,
    SPAM,
    SUSPICIOUS,
    VERIFIED,
    CLEAN_OBSERVED,
    UNKNOWN,
    OFFLINE
}

enum class ConfidenceTier {
    HIGH,
    MEDIUM,
    LOW,
    NONE
}

data class ScreeningLookupResult(
    val phoneTag: CachedPhoneTag,
    val source: String, // SERVER, FIXTURE, CACHE
    val lookupLatencyMs: Double = 0.0,
    val availability: LookupAvailability = LookupAvailability.AVAILABLE,
    val displayStatus: LookupDisplayStatus = LookupDisplayStatus.UNKNOWN,
    val isStale: Boolean = false
)

data class FullPhoneLookupResult(
    val phoneTag: CachedPhoneTag,
    val categoryBreakdown: Map<String, Int>,
    val recentReports: List<SpamReportItemDto>,
    val source: String, // SERVER, FIXTURE, CACHE, OFFLINE
    val lookupLatencyMs: Double,
    val availability: LookupAvailability = LookupAvailability.AVAILABLE,
    val displayStatus: LookupDisplayStatus = LookupDisplayStatus.UNKNOWN,
    val confidence: ConfidenceTier = ConfidenceTier.NONE,
    val isStale: Boolean = false,
    val statusHeadline: String = "Belum ada data tentang nomor ini",
    val statusSubtext: String = "",
    val verifiedLabel: String? = null,
    val fetchedAtEpochMs: Long = phoneTag.fetchedAtEpochMs,
    val verifiedSourceUrl: String? = null,
    val verifiedAt: String? = null,
    val verifiedExpiresAt: String? = null,
    val tags: List<id.calltag.app.data.remote.dto.TagCountDto> = emptyList()
)

data class OfflineCacheStats(
    val syncVersion: Long,
    val cachedPhoneCount: Int,
    val emergencyContactsCount: Int
)

/**
 * Repository utama berarsitektur Offline-First yang menggabungkan Room Database (Local Cache)
 * dan Retrofit `ApiService` sesuai kontrak resmi `CallTag` v2.
 */
class CallTagRepository(
    private val phoneTagDao: PhoneTagDao,
    private val emergencyContactDao: EmergencyContactDao,
    private val callScreeningLogDao: CallScreeningLogDao,
    private val apiService: ApiService,
    private val fastScreeningApiService: ApiService,
    private val preferences: CallTagPreferences,
    private val pendingReportDao: PendingReportDao? = null,
    private val pendingDisputeDao: PendingDisputeDao? = null,
    private val syncStateDao: SyncStateDao? = null
) {

    private val sessionOperations = Mutex()

    /** Pengguna yang belum masuk tidak didaftarkan otomatis ke backend. */
    suspend fun ensureDeviceAuthenticated(): Result<String> = runCatching {
        check(preferences.isLoggedInSync()) { "Masuk akun untuk membuka fitur ini." }
        preferences.getAccessTokenSync().also { check(it.isNotBlank()) { "Sesi berakhir. Silakan masuk kembali." } }
    }

    suspend fun logout() = sessionOperations.withLock {
        val owner = preferences.sessionIdSync()
        pendingReportDao?.observeAll()?.first()?.filter {
            SessionAccessPolicy.canAccessOwnedReport(owner, preferences.reportOwnerSync(it.localId)) && it.status in setOf("PENDING", "SENDING")
        }?.forEach { pendingReportDao?.update(it.copy(status = "PAUSED", lastError = "Sesi berakhir; laporan tidak dikirim.")) }
        preferences.clearSession()
    }

    suspend fun clearLocalCache() {
        phoneTagDao.deleteAllServerCache()
        syncStateDao?.upsert(SyncStateEntity(id = 1))
    }

    suspend fun checkBackendHealth(): Result<String> = runCatching {
        val response = withTimeoutOrNull(2500L) { apiService.getHealth() }
            ?: error("Backend tidak merespons.")
        response.body()?.close()
        response.errorBody()?.close()
        check(response.isSuccessful) { "Backend bermasalah (HTTP ${response.code()})." }
        "Backend terhubung"
    }

    /**
     * Digunakan oleh CallTagScreeningService saat panggilan masuk berdering:
     * 1. Cek Room Database lokal terlebih dahulu (< 20 ms).
     * 2. Jika belum ada di Room DB dan perangkat online, panggil `GET /api/v1/phones/{e164}`
     *    dengan batas timeout ketat 800 ms, lalu simpan hasilnya bila `found == true`.
     */
    suspend fun screenIncomingPhone(rawPhone: String): ScreeningLookupResult? {
        if (rawPhone.contains('*')) return null
        val e164 = PhoneNormalizer.normalizeToE164(rawPhone)
        if (e164.isEmpty()) return null
        if (PhoneNormalizer.isEmergencyOrShortCode(rawPhone)) return null

        val now = System.currentTimeMillis()
        var cached: CachedPhoneTag? = null
        val elapsedNanos = measureNanoTime {
            cached = phoneTagDao.getByPhone(e164)
        }
        val localMs = (elapsedNanos / 1_000_000.0)
        val localHit = cached?.takeUnless { it.source.equals("FIXTURE", ignoreCase = true) }
        if (localHit != null) {
            val resolved = resolveLookupResult(e164, LookupAvailability.AVAILABLE, localHit.copy(verifiedLabel = null), now)
            return ScreeningLookupResult(
                phoneTag = localHit,
                source = localHit.source,
                lookupLatencyMs = localMs,
                availability = resolved.availability,
                displayStatus = resolved.displayStatus,
                isStale = resolved.isStale
            )
        }

        // Batas timeout ketat 800 ms ke endpoint resmi GET /api/v1/phones/{e164}
        val remoteDto = withTimeoutOrNull(800L) {
            runCatching {
                fastScreeningApiService.getPhoneDetails(e164).data
            }.getOrNull()
        } ?: return null

        if (!remoteDto.found) return null

        val entity = mapLookupDtoToEntity(e164, remoteDto, now)
        phoneTagDao.upsert(entity)
        val resolved = resolveLookupResult(e164, LookupAvailability.AVAILABLE, entity, now)
        return ScreeningLookupResult(
            phoneTag = entity,
            source = "SERVER",
            lookupLatencyMs = localMs,
            availability = LookupAvailability.AVAILABLE,
            displayStatus = resolved.displayStatus,
            isStale = false
        )
    }

    /**
     * Pencarian manual nomor telepon dari HomeScreen (model 2 dimensi T-303):
     * - Tidak pernah mengarang status `CLEAN` atau `"Nomor Aman"` ketika nomor belum terdaftar atau offline.
     */
    suspend fun lookupPhoneFull(rawPhone: String): FullPhoneLookupResult? {
        if (rawPhone.contains('*')) return null
        val e164 = PhoneNormalizer.normalizeToE164(rawPhone)
        if (e164.isEmpty()) return null

        val now = System.currentTimeMillis()
        var localTag: CachedPhoneTag? = null
        val localNanos = measureNanoTime {
            localTag = phoneTagDao.getByPhone(e164)
        }
        val localLatencyMs = (localNanos / 1_000_000.0)

        var networkUnavailable = false
        val remoteDetails: LookupDataDto? = withTimeoutOrNull(1800L) {
            try {
                apiService.getPhoneDetails(e164).data
            } catch (e: HttpException) {
                if (e.code() == 404) {
                    LookupDataDto(phoneE164 = e164, found = false)
                } else {
                    networkUnavailable = true
                    null
                }
            } catch (_: Exception) {
                networkUnavailable = true
                null
            }
        } ?: run {
            networkUnavailable = true
            null
        }

        if (remoteDetails != null && remoteDetails.found) {
            val remoteReports = withTimeoutOrNull(1800L) {
                runCatching { apiService.getPhoneReports(e164).data }.getOrNull()
            }
            val entity = mapLookupDtoToEntity(e164, remoteDetails, now)
            phoneTagDao.upsert(entity)
            val reportsList = remoteReports?.reports?.ifEmpty { remoteDetails.recentReports }
                ?: remoteDetails.recentReports
            val verificationActive = remoteDetails.isWhitelistedVerified &&
                PhoneNormalizer.isVerificationUnexpired(remoteDetails.verifiedExpiresAt, now)
            return resolveLookupResult(
                e164 = e164,
                availability = LookupAvailability.AVAILABLE,
                entity = entity.copy(verifiedLabel = remoteDetails.verifiedLabel.takeIf { verificationActive }
                    ?: "Nomor Resmi Terverifikasi".takeIf { verificationActive }),
                nowEpochMs = now,
                categoryBreakdown = remoteDetails.categoryBreakdown,
                recentReports = reportsList,
                lookupLatencyMs = localLatencyMs
            ).copy(
                verifiedSourceUrl = remoteDetails.verifiedSourceUrl,
                verifiedAt = remoteDetails.verifiedAt,
                verifiedExpiresAt = remoteDetails.verifiedExpiresAt,
                tags = remoteDetails.tags
            )
        }

        if (!networkUnavailable && remoteDetails != null && !remoteDetails.found) {
            val currentLocal = localTag
            if (currentLocal != null && !currentLocal.source.equals("FIXTURE", ignoreCase = true)) {
                phoneTagDao.deleteByPhone(e164)
            }
            return resolveLookupResult(
                e164 = e164,
                availability = LookupAvailability.NOT_FOUND,
                entity = null,
                nowEpochMs = now,
                lookupLatencyMs = localLatencyMs
            )
        }

        // Perangkat offline atau server tidak terjangkau
        val currentLocal = localTag
        return if (currentLocal != null) {
            resolveLookupResult(
                e164 = e164,
                availability = LookupAvailability.UNAVAILABLE,
                entity = currentLocal,
                nowEpochMs = now,
                categoryBreakdown = if (currentLocal.source == "FIXTURE") {
                    OfflineSeedData.getOfflineBreakdownForPhone(currentLocal)
                } else emptyMap(),
                recentReports = if (currentLocal.source == "FIXTURE") {
                    OfflineSeedData.getOfflineCommentsForPhone(currentLocal)
                } else emptyList(),
                lookupLatencyMs = localLatencyMs
            )
        } else {
            resolveLookupResult(
                e164 = e164,
                availability = LookupAvailability.UNAVAILABLE,
                entity = null,
                nowEpochMs = now,
                lookupLatencyMs = localLatencyMs
            )
        }
    }

    /**
     * Mengirim laporan spam/scam & tag nama baru ke `POST /api/v1/reports` (T-304).
     * Laporan disimpan di outbox `pending_reports` dengan `Idempotency-Key` stabil, dan
     * TIDAK pernah memanipulasi skor agregat lokal pada `cached_phone_tags` (mitigasi A04).
     */
    suspend fun submitSpamReport(
        phone: String,
        categoryCode: String,
        tagName: String,
        comment: String,
        callType: String = "CELLULAR_CALL",
        deviceId: String? = null
    ): Result<ReportResponseDto> = sessionOperations.withLock { runCatching {
        val token = ensureDeviceAuthenticated().getOrThrow()
        val owner = preferences.sessionIdSync()
        val normalizedE164 = PhoneNormalizer.normalizeToE164(phone)
        require(normalizedE164.length >= 8) { "Nomor telepon tidak valid (gunakan format 08xx atau +628xx)" }
        require(tagName.isNotBlank()) { "Label tag komunitas wajib diisi" }

        val validCat = if (categoryCode.uppercase() in ScamCategories.ALL) {
            categoryCode.uppercase()
        } else {
            ScamCategories.TELEMARKETING
        }

        val now = System.currentTimeMillis()
        val idempotencyKey = UUID.randomUUID().toString()
        val installId = deviceId?.takeIf { it.isNotBlank() } ?: preferences.getOrCreateInstallIdSync()

        var outboxId = 0L
        val outboxEntry = PendingReportEntity(
            idempotencyKey = idempotencyKey,
            phoneE164 = normalizedE164,
            categoryCode = validCat,
            customTag = tagName.trim(),
            comment = comment.trim(),
            callType = callType,
            status = "SENDING",
            attempts = 1,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        if (pendingReportDao != null) {
            outboxId = pendingReportDao.insert(outboxEntry)
            preferences.ownReport(outboxId, owner)
        }

        val bearer = if (token.isNotBlank()) "Bearer $token" else null
        val req = ReportRequestDto(
            phone = normalizedE164,
            categoryCode = validCat,
            customTag = tagName.trim(),
            comment = comment.trim(),
            callType = callType
        )

        try {
            val remoteResult = apiService.submitReport(
                bearerToken = bearer,
                deviceId = installId,
                idempotencyKey = idempotencyKey,
                request = req
            ).data ?: error("Respons server kosong")

            if (pendingReportDao != null && outboxId > 0L) {
                pendingReportDao.update(
                    outboxEntry.copy(
                        localId = outboxId,
                        status = "SENT",
                        remoteReportId = remoteResult.reportId,
                        lastError = "",
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                )
            }

            val pt = remoteResult.phoneTag
            val cached = CachedPhoneTag(
                phoneE164 = pt.phoneE164.ifBlank { normalizedE164 },
                carrier = pt.carrier.ifBlank { "Operator Telekomunikasi Indonesia" },
                reputationLevel = pt.reputationLevel,
                spamScore = pt.spamScore,
                isDanger = pt.isDanger,
                primaryCategory = pt.primaryCategory,
                primaryCategoryLabel = pt.primaryCategoryLabel.ifBlank {
                    PhoneNormalizer.getCategoryDisplayLabel(pt.primaryCategory)
                },
                topCommunityLabel = pt.topCommunityLabel.ifBlank { tagName.trim() },
                tagsSummary = pt.tags.joinToString(", ") { "${it.tag} (${it.count})" },
                uniqueReporters = pt.uniqueReporters,
                syncVersion = pt.syncVersion,
                cachedAtEpochMs = System.currentTimeMillis(),
                availability = LookupAvailability.AVAILABLE.name,
                source = "SERVER",
                fetchedAtEpochMs = System.currentTimeMillis(),
                expiresAtEpochMs = System.currentTimeMillis() + PhoneNormalizer.CACHE_DISPLAY_TTL_MS,
                serverVersion = pt.syncVersion,
                verifiedLabel = if (pt.isWhitelistedVerified) "Nomor Resmi Terverifikasi" else null
            )
            phoneTagDao.upsert(cached)
            remoteResult
        } catch (e: HttpException) {
            val code = e.code()
            val permanentClientReject = code in 400..499 && code != 401 && code != 408 && code != 429
            if (permanentClientReject) {
                if (pendingReportDao != null && outboxId > 0L) {
                    pendingReportDao.update(
                        outboxEntry.copy(
                            localId = outboxId,
                            status = "FAILED",
                            attempts = 99,
                            lastError = "Ditolak server (HTTP $code)",
                            updatedAtEpochMs = System.currentTimeMillis()
                        )
                    )
                }
                throw IllegalStateException("Laporan ditolak server (HTTP $code): periksa isi laporan atau batas kuota")
            }

            if (pendingReportDao != null && outboxId > 0L) {
                pendingReportDao.update(
                    outboxEntry.copy(
                        localId = outboxId,
                        status = "PENDING",
                        lastError = "Menunggu koneksi / server (HTTP $code)",
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                )
            }
            buildQueuedReportResponse(normalizedE164, validCat, tagName.trim(), idempotencyKey)
        } catch (e: Exception) {
            if (pendingReportDao != null && outboxId > 0L) {
                pendingReportDao.update(
                    outboxEntry.copy(
                        localId = outboxId,
                        status = "PENDING",
                        lastError = e.message ?: "Offline, menunggu jaringan",
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                )
            }
            buildQueuedReportResponse(normalizedE164, validCat, tagName.trim(), idempotencyKey)
        }
    } }

    private fun buildQueuedReportResponse(
        normalizedE164: String,
        validCat: String,
        tagName: String,
        idempotencyKey: String
    ): ReportResponseDto {
        val recommended = OfflineSeedData.getInitialOfficialChannels().filter {
            validCat in it.relatedCategories
        }.ifEmpty { OfflineSeedData.getInitialOfficialChannels() }
        return ReportResponseDto(
            reportId = "PENDING:$idempotencyKey",
            phoneTag = PhoneTagDto(
                phoneE164 = normalizedE164,
                carrier = "Operator Telekomunikasi Indonesia",
                reputationLevel = "PENDING_QUEUE",
                spamScore = 0.0,
                primaryCategory = validCat,
                primaryCategoryLabel = PhoneNormalizer.getCategoryDisplayLabel(validCat),
                isDanger = false,
                topCommunityLabel = tagName,
                uniqueReporters = 0,
                totalReports = 0,
                syncVersion = 0L
            ),
            recommendedOfficialHub = recommended
        )
    }

    /**
     * Mengirim ulang seluruh laporan offline pada tabel `pending_reports` secara idempoten (T-304).
     */
    suspend fun flushPendingReports(): Int = sessionOperations.withLock {
        if (!preferences.isLoggedInSync()) return@withLock 0
        val owner = preferences.sessionIdSync()
        val dao = pendingReportDao ?: return@withLock 0
        dao.recoverSendingToPending()
        val retryable = dao.getRetryable(maxAttempts = 8).filter { SessionAccessPolicy.canAccessOwnedReport(owner, preferences.reportOwnerSync(it.localId)) }
        if (retryable.isEmpty()) return@withLock 0

        val installId = preferences.getOrCreateInstallIdSync()
        var sentCount = 0

        for (item in retryable) {
            dao.update(item.copy(status = "SENDING", updatedAtEpochMs = System.currentTimeMillis()))
            val token = preferences.getAccessTokenSync()
            val bearer = if (token.isNotBlank()) "Bearer $token" else null
            val req = ReportRequestDto(
                phone = item.phoneE164,
                categoryCode = item.categoryCode,
                customTag = item.customTag,
                comment = item.comment,
                callType = item.callType
            )
            try {
                val remote = apiService.submitReport(
                    bearerToken = bearer,
                    deviceId = installId,
                    idempotencyKey = item.idempotencyKey,
                    request = req
                ).data ?: error("Respons server kosong")

                val now = System.currentTimeMillis()
                dao.update(
                    item.copy(
                        status = "SENT",
                        attempts = item.attempts + 1,
                        remoteReportId = remote.reportId,
                        lastError = "",
                        updatedAtEpochMs = now
                    )
                )
                val pt = remote.phoneTag
                if (pt.phoneE164.isNotBlank()) {
                    phoneTagDao.upsert(
                        CachedPhoneTag(
                            phoneE164 = pt.phoneE164,
                            carrier = pt.carrier.ifBlank { "Operator Telekomunikasi Indonesia" },
                            reputationLevel = pt.reputationLevel,
                            spamScore = pt.spamScore,
                            isDanger = pt.isDanger,
                            primaryCategory = pt.primaryCategory,
                            primaryCategoryLabel = pt.primaryCategoryLabel.ifBlank {
                                PhoneNormalizer.getCategoryDisplayLabel(pt.primaryCategory)
                            },
                            topCommunityLabel = pt.topCommunityLabel,
                            tagsSummary = pt.tags.joinToString(", ") { "${it.tag} (${it.count})" },
                            uniqueReporters = pt.uniqueReporters,
                            syncVersion = pt.syncVersion,
                            cachedAtEpochMs = now,
                            availability = LookupAvailability.AVAILABLE.name,
                            source = "SERVER",
                            fetchedAtEpochMs = now,
                            expiresAtEpochMs = now + PhoneNormalizer.CACHE_DISPLAY_TTL_MS,
                            serverVersion = pt.syncVersion,
                            verifiedLabel = if (pt.isWhitelistedVerified) "Nomor Resmi Terverifikasi" else null
                        )
                    )
                }
                sentCount++
            } catch (e: HttpException) {
                val code = e.code()
                val permanentReject = code in 400..499 && code != 401 && code != 408 && code != 429
                val nextAttempts = if (permanentReject) 99 else item.attempts + 1
                val nextStatus = if (permanentReject || nextAttempts >= 8) "FAILED" else "PENDING"
                dao.update(
                    item.copy(
                        status = nextStatus,
                        attempts = nextAttempts,
                        lastError = "HTTP $code",
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                val nextAttempts = item.attempts + 1
                val nextStatus = if (nextAttempts >= 8) "FAILED" else "PENDING"
                dao.update(
                    item.copy(
                        status = nextStatus,
                        attempts = nextAttempts,
                        lastError = e.message ?: "Gagal mengirim ulang",
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                )
            }
        }
        sentCount
    }

    suspend fun retryPendingReport(localId: Long): Int {
        val dao = pendingReportDao ?: return 0
        val current = dao.getById(localId) ?: return 0
        if (!preferences.isLoggedInSync() || preferences.reportOwnerSync(localId) != preferences.sessionIdSync()) return 0
        dao.update(current.copy(status = "PENDING", attempts = 0, lastError = "", updatedAtEpochMs = System.currentTimeMillis()))
        return flushPendingReports()
    }

    suspend fun deletePendingReport(localId: Long) {
        val owner = preferences.sessionIdSync()
        if (SessionAccessPolicy.canAccessOwnedReport(owner, preferences.reportOwnerSync(localId))) {
            pendingReportDao?.deleteById(localId)
        }
    }

    fun observePendingReports(): Flow<List<PendingReportEntity>> = combine(
        preferences.sessionRevision, pendingReportDao?.observeAll() ?: flowOf(emptyList())
    ) { _, reports ->
        val owner = preferences.sessionIdSync()
        reports.asSequence().filter {
            SessionAccessPolicy.canAccessOwnedReport(owner, preferences.reportOwnerSync(it.localId))
        }.take(50).toList()
    }

    private fun disputeOwnerScope(): String =
        preferences.sessionIdSync().takeIf { it.isNotBlank() }?.let { "account:$it" } ?: "unregistered"

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observePendingDisputes(): Flow<List<PendingDisputeEntity>> =
        preferences.sessionRevision.flatMapLatest {
            pendingDisputeDao?.observeAll(disputeOwnerScope()) ?: flowOf(emptyList())
        }

    /**
     * Mengajukan sanggahan (dispute) nomor telepon ke `POST /api/v1/disputes` (US-05 / T-307).
     * Menyimpan `tracking_token` secara lokal di `pending_disputes` untuk pelacakan status tanpa login.
     */
    suspend fun submitDispute(
        phone: String,
        ownerName: String,
        reason: String,
        contactRef: String
    ): Result<DisputeResponseDto> = sessionOperations.withLock {
        runCatching {
            val e164 = PhoneNormalizer.normalizeToE164(phone)
            require(e164.length >= 8) { "Nomor telepon yang diajukan tidak valid" }
            require(ownerName.isNotBlank()) { "Nama pemilik/usaha wajib diisi" }
            require(reason.isNotBlank()) { "Alasan sanggahan wajib diisi" }

            val now = System.currentTimeMillis()
            val entry = PendingDisputeEntity(
                ownerScope = disputeOwnerScope(),
                phoneE164 = e164,
                ownerName = ownerName.trim(),
                reason = reason.trim(),
                contactRef = contactRef.trim(),
                status = "SENDING",
                attempts = 1,
                createdAtEpochMs = now
            )
            val localId = pendingDisputeDao?.insert(entry) ?: 0L

            val token = preferences.getAccessTokenSync()
            val bearer = if (token.isNotBlank()) "Bearer $token" else null
            val installId = preferences.getOrCreateInstallIdSync()
            val req = DisputeRequestDto(
                phone = e164,
                ownerName = ownerName.trim(),
                reason = reason.trim(),
                contactRef = contactRef.trim()
            )

            try {
                val remote = apiService.submitDispute(
                    bearerToken = bearer,
                    deviceId = installId,
                    request = req
                ).data ?: error("Respons sanggahan kosong")

                if (pendingDisputeDao != null && localId > 0L) {
                    pendingDisputeDao.update(
                        entry.copy(
                            localId = localId,
                            status = remote.status.ifBlank { "UNDER_REVIEW" },
                            disputeId = remote.disputeId,
                            trackingToken = remote.trackingToken,
                            lastError = ""
                        )
                    )
                }
                remote
            } catch (e: HttpException) {
                val code = e.code()
                val permanentReject = code in 400..499 && code != 401 && code != 408 && code != 429
                if (permanentReject) {
                    if (pendingDisputeDao != null && localId > 0L) {
                        pendingDisputeDao.update(
                            entry.copy(
                                localId = localId,
                                status = "FAILED",
                                attempts = 99,
                                lastError = "HTTP $code"
                            )
                        )
                    }
                    val msg = if (code == 409) {
                        "Sudah terdapat pengajuan sanggahan aktif untuk nomor ini (409 Conflict)."
                    } else {
                        "Pengajuan sanggahan ditolak server (HTTP $code)."
                    }
                    throw IllegalStateException(msg)
                }
                if (pendingDisputeDao != null && localId > 0L) {
                    pendingDisputeDao.update(
                        entry.copy(localId = localId, status = "PENDING", lastError = "HTTP $code")
                    )
                }
                DisputeResponseDto(
                    disputeId = "PENDING_$localId",
                    phoneE164 = e164,
                    ownerName = ownerName.trim(),
                    reason = reason.trim(),
                    status = "PENDING",
                    submittedAt = "Tersimpan di antrean offline CallTag"
                )
            } catch (e: Exception) {
                if (pendingDisputeDao != null && localId > 0L) {
                    pendingDisputeDao.update(
                        entry.copy(
                            localId = localId,
                            status = "PENDING",
                            lastError = e.message ?: "Offline"
                        )
                    )
                }
                DisputeResponseDto(
                    disputeId = "PENDING_$localId",
                    phoneE164 = e164,
                    ownerName = ownerName.trim(),
                    reason = reason.trim(),
                    status = "PENDING",
                    submittedAt = "Tersimpan di antrean offline CallTag"
                )
            }
        }
    }

    suspend fun flushPendingDisputes(): Int = sessionOperations.withLock {
        val dao = pendingDisputeDao ?: return@withLock 0
        val ownerScope = disputeOwnerScope()
        dao.recoverSendingToPending(ownerScope)
        val retryable = dao.getRetryable(ownerScope, maxAttempts = 8)
        if (retryable.isEmpty()) return@withLock 0

        val installId = preferences.getOrCreateInstallIdSync()
        var synced = 0
        for (item in retryable) {
            dao.update(item.copy(status = "SENDING"))
            val token = preferences.getAccessTokenSync()
            val bearer = if (token.isNotBlank()) "Bearer $token" else null
            val req = DisputeRequestDto(
                phone = item.phoneE164,
                ownerName = item.ownerName,
                reason = item.reason,
                contactRef = item.contactRef
            )
            try {
                val remote = apiService.submitDispute(bearer, installId, req).data ?: error("Kosong")
                dao.update(
                    item.copy(
                        status = remote.status.ifBlank { "UNDER_REVIEW" },
                        disputeId = remote.disputeId,
                        trackingToken = remote.trackingToken,
                        attempts = item.attempts + 1,
                        lastError = ""
                    )
                )
                synced++
            } catch (e: HttpException) {
                val permanentReject = e.code() in 400..499 && e.code() != 401 && e.code() != 408 && e.code() != 429
                val nextAttempts = if (permanentReject) 99 else item.attempts + 1
                dao.update(
                    item.copy(
                        status = if (permanentReject || nextAttempts >= 8) "FAILED" else "PENDING",
                        attempts = nextAttempts,
                        lastError = "HTTP ${e.code()}"
                    )
                )
            } catch (e: Exception) {
                val nextAttempts = item.attempts + 1
                dao.update(
                    item.copy(
                        status = if (nextAttempts >= 8) "FAILED" else "PENDING",
                        attempts = nextAttempts,
                        lastError = e.message ?: "Gagal kirim"
                    )
                )
            }
        }
        synced
    }

    /**
     * Memeriksa status sanggahan via `GET /api/v1/disputes/{id}` beserta header opsional `X-Tracking-Token`.
     */
    suspend fun checkDisputeStatus(
        disputeId: String,
        trackingToken: String? = null
    ): Result<DisputeResponseDto> = runCatching {
        val cleanedId = disputeId.trim()
        require(cleanedId.isNotBlank()) { "ID sanggahan wajib diisi" }
        val token = preferences.getAccessTokenSync()
        val bearer = if (token.isNotBlank()) "Bearer $token" else null
        val tracking = trackingToken?.trim()?.takeIf { it.isNotBlank() }
        val resp = apiService.getDisputeStatus(
            disputeId = cleanedId,
            trackingToken = tracking,
            bearerToken = bearer
        )
        resp.data ?: error("Data status sanggahan tidak ditemukan")
    }

    /**
     * Sinkronisasi delta menggunakan cursor opak `GET /api/v1/sync/changes?cursor=&limit=` (T-304).
     * Menangani HTTP `409 FULL_RESYNC_REQUIRED` dengan membersihkan cache server lokal lalu resync.
     */
    suspend fun syncDeltaChanges(): Result<OfflineCacheStats> = runCatching {
        val stateDao = syncStateDao
        val currentState = stateDao?.get() ?: SyncStateEntity()
        var cursor = currentState.cursor
        var generation = currentState.generation
        var lastSyncedAt = currentState.lastSuccessAtEpochMs
        var page = 0
        var resetRetried = false

        while (page < 20) {
            val response = apiService.getSyncChanges(cursor = cursor.ifBlank { null }, limit = 200)
            if (response.code() == 409) {
                if (resetRetried) error("FULL_RESYNC_REQUIRED berulang saat sinkronisasi")
                resetRetried = true
                phoneTagDao.deleteAllServerCache()
                cursor = ""
                stateDao?.upsert(
                    SyncStateEntity(
                        id = 1,
                        cursor = "",
                        generation = generation + 1,
                        lastSuccessAtEpochMs = lastSyncedAt,
                        lastError = "FULL_RESYNC_REQUIRED"
                    )
                )
                continue
            }

            if (!response.isSuccessful) {
                val msg = "Sync gagal HTTP ${response.code()}"
                stateDao?.upsert(
                    SyncStateEntity(
                        id = 1,
                        cursor = cursor,
                        generation = generation,
                        lastSuccessAtEpochMs = lastSyncedAt,
                        lastError = msg
                    )
                )
                error(msg)
            }

            val data = response.body()?.data ?: break
            val now = System.currentTimeMillis()
            generation = data.generation

            for (item in data.items) {
                if (!item.kind.equals("PHONE_TAG", ignoreCase = true)) continue
                if (item.op.equals("DELETE", ignoreCase = true)) {
                    phoneTagDao.deleteByPhone(item.key)
                    continue
                }
                val payload = item.payload ?: continue
                val phoneE164 = (payload["phone_e164"] as? String)?.ifBlank { item.key } ?: item.key
                if (phoneE164.isBlank()) continue
                val repLevel = (payload["reputation_level"] as? String).orEmpty().ifBlank { ReputationLevels.SUSPICIOUS }
                val score = (payload["risk_score"] as? Number)?.toDouble()
                    ?: (payload["spam_score"] as? Number)?.toDouble()
                    ?: 0.0
                val isDanger = (payload["is_danger"] as? Boolean)
                    ?: (repLevel == ReputationLevels.DANGER_SCAM)
                val primaryCat = (payload["primary_category"] as? String).orEmpty()
                val primaryCatLabel = (payload["primary_category_label"] as? String)
                    ?.ifBlank { PhoneNormalizer.getCategoryDisplayLabel(primaryCat) }
                    ?: PhoneNormalizer.getCategoryDisplayLabel(primaryCat)
                val topLabel = (payload["top_community_label"] as? String).orEmpty()
                val reporters = (payload["unique_reporters"] as? Number)?.toInt() ?: 0
                val syncVer = (payload["sync_version"] as? Number)?.toLong() ?: item.seq

                phoneTagDao.upsert(
                    CachedPhoneTag(
                        phoneE164 = phoneE164,
                        carrier = "Operator Telekomunikasi Indonesia",
                        reputationLevel = repLevel,
                        spamScore = score,
                        isDanger = isDanger,
                        primaryCategory = primaryCat,
                        primaryCategoryLabel = primaryCatLabel,
                        topCommunityLabel = topLabel,
                        tagsSummary = topLabel,
                        uniqueReporters = reporters,
                        syncVersion = syncVer,
                        cachedAtEpochMs = now,
                        availability = LookupAvailability.AVAILABLE.name,
                        source = "SERVER",
                        fetchedAtEpochMs = now,
                        expiresAtEpochMs = now + PhoneNormalizer.CACHE_DISPLAY_TTL_MS,
                        serverVersion = syncVer
                    )
                )
            }

            if (data.nextCursor.isNotBlank()) {
                cursor = data.nextCursor
            }
            lastSyncedAt = now
            stateDao?.upsert(
                SyncStateEntity(
                    id = 1,
                    cursor = cursor,
                    generation = generation,
                    lastSuccessAtEpochMs = lastSyncedAt,
                    lastError = ""
                )
            )
            page++
            if (!data.hasMore || data.nextCursor.isBlank()) break
        }

        // Segarkan kontak darurat nasional secara best-effort
        val natContacts = runCatching { apiService.getNationalEmergencyDirectory().data?.nationalNumbers }.getOrNull()
        if (!natContacts.isNullOrEmpty()) {
            emergencyContactDao.upsertAll(natContacts.map { mapEmergencyDtoToEntity(it) })
        }

        getLocalCacheStats()
    }

    /**
     * Alias kompatibilitas yang memicu sinkronisasi delta cursor-based `/api/v1/sync/changes`.
     */
    suspend fun syncTopScammersToLocalCache(): Result<OfflineCacheStats> = syncDeltaChanges()

    suspend fun getLocalCacheStats(): OfflineCacheStats {
        if (emergencyContactDao.count() == 0) {
            emergencyContactDao.upsertAll(OfflineSeedData.getInitialEmergencyContacts())
        }
        if (BuildConfig.DEBUG && phoneTagDao.count() == 0) {
            phoneTagDao.upsertAll(OfflineSeedData.getInitialTopScammers())
        }
        return OfflineCacheStats(
            syncVersion = phoneTagDao.getLatestSyncVersion(),
            cachedPhoneCount = phoneTagDao.count(),
            emergencyContactsCount = emergencyContactDao.count()
        )
    }

    /**
     * Mengambil daftar kanal pelaporan resmi pemerintah (`GET /api/v1/official-channels`)
     */
    suspend fun getOfficialChannels(category: String? = null): List<OfficialChannelDto> {
        val remote = withTimeoutOrNull(1500L) {
            runCatching {
                apiService.getOfficialChannels(category = category).data
            }.getOrNull()
        }
        if (remote != null) return remote

        val seeded = OfflineSeedData.getInitialOfficialChannels()
        if (category.isNullOrBlank()) return seeded
        return seeded.filter { category in it.relatedCategories }.ifEmpty { seeded }
    }

    /**
     * Sinkronisasi daftar nasional dan lokal dari `GET /api/v1/emergency/local` ke Room DB.
     */
    suspend fun syncEmergencyDirectory(): Result<Unit> = runCatching {
        val response = withTimeoutOrNull(2500L) {
            apiService.getLocalEmergencyDirectory().data
        } ?: error("Direktori darurat dari server tidak tersedia")

        // Respons tanpa filter berisi daftar lengkap. Pertahankan kontak nasional cadangan
        // jika server tidak mengirim daftar nasional yang dapat dipakai.
        val national = response.nationalNumbers.map(::mapEmergencyDtoToEntity)
        if (national.isNotEmpty()) emergencyContactDao.replaceScope("NATIONAL", national)
        emergencyContactDao.replaceScope(
            "LOCAL",
            response.localDirectory.map(::mapEmergencyDtoToEntity)
        )
    }

    suspend fun authenticateGoogle(idToken: String): Result<Unit> = sessionOperations.withLock {
        runCatching {
            val apiUrl = android.net.Uri.parse(BuildConfig.BASE_API_URL)
            val developmentLoopback = BuildConfig.DEBUG &&
                apiUrl.host in setOf("127.0.0.1", "localhost", "10.0.2.2")
            require(apiUrl.scheme == "https" || developmentLoopback) {
                "Untuk masuk dengan Google, gunakan koneksi USB saat tes lokal atau API HTTPS."
            }
            val response = apiService.authenticateGoogle(GoogleLoginRequestDto(idToken))
            val data = response.data ?: error("Respons login kosong.")
            val access = data.accessToken.ifBlank { data.token }
            require(access.isNotBlank() && data.refreshToken.isNotBlank() && data.user.id.isNotBlank()) {
                "Sesi CallTag belum lengkap."
            }
            preferences.startExplicitSession(
                access, data.refreshToken, data.user.displayName,
                preferences.getOrCreateInstallIdSync(), data.user.id
            )
        }
    }

    /**
     * Mengambil profil & sisa kuota pengguna dari `GET /api/v1/me` (T-307).
     */
    suspend fun fetchAccountProfile(): Result<MeResponseDto> = runCatching {
        val token = ensureDeviceAuthenticated().getOrThrow()
        val bearer = if (token.isNotBlank()) "Bearer $token" else null
        val resp = apiService.getMe(
            bearerToken = bearer,
            deviceId = preferences.getOrCreateInstallIdSync()
        )
        resp.data ?: error("Data profil akun kosong")
    }

    /**
     * Mengajukan penghapusan data akun (UU PDP) melalui `DELETE /api/v1/me` (T-307).
     * Laporan teragregasi tetap dianonimkan di server sementara sesi perangkat dihapus.
     */
    suspend fun requestAccountDeletion(): Result<String> = runCatching {
        val token = ensureDeviceAuthenticated().getOrThrow()
        val bearer = if (token.isNotBlank()) "Bearer $token" else null
        val resp = apiService.deleteMe(
            bearerToken = bearer,
            deviceId = preferences.getOrCreateInstallIdSync()
        )
        if (!resp.isSuccessful) {
            error("Permintaan penghapusan gagal (HTTP ${resp.code()})")
        }
        logout()
        "Permintaan penghapusan akun diterima (UU PDP). Token sesi perangkat telah dihapus."
    }

    fun observeNationalEmergencyContacts(): Flow<List<EmergencyContact>> =
        emergencyContactDao.observeNationalContacts()

    fun observeLocalEmergencyContacts(
        province: String = "",
        city: String = "",
        agencyType: String = ""
    ): Flow<List<EmergencyContact>> =
        emergencyContactDao.observeLocalContacts(province, city, agencyType)

    fun observeDangerScammers(): Flow<List<CachedPhoneTag>> =
        phoneTagDao.observeDangerPhoneTags()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeRecentScreeningLogs(): Flow<List<CallScreeningLog>> =
        preferences.sessionRevision.flatMapLatest {
            val owner = preferences.sessionIdSync()
            if (owner.isBlank()) flowOf(emptyList())
            else callScreeningLogDao.observeRecentLogs(owner)
        }

    suspend fun recordScreeningLog(log: CallScreeningLog, owner: String) {
        if (owner.isBlank()) return
        val masked = if (log.phoneE164.contains('*')) log.phoneE164 else PhoneNormalizer.maskPhone(log.phoneE164)
        callScreeningLogDao.insert(log.copy(phoneE164 = masked, ownerScope = owner))
    }

    private fun mapLookupDtoToEntity(
        e164: String,
        dto: LookupDataDto,
        nowEpochMs: Long = System.currentTimeMillis()
    ): CachedPhoneTag {
        val label = dto.topCommunityLabel.ifBlank {
            PhoneNormalizer.getCategoryDisplayLabel(dto.primaryCategory)
        }
        return CachedPhoneTag(
            phoneE164 = dto.phoneE164.ifBlank { e164 },
            carrier = dto.carrier.ifBlank { PhoneNormalizer.detectCarrierContext(e164) },
            reputationLevel = dto.reputationLevel.ifBlank { ReputationLevels.SUSPICIOUS },
            spamScore = dto.spamScore,
            isDanger = dto.isDanger,
            primaryCategory = dto.primaryCategory,
            primaryCategoryLabel = dto.primaryCategoryLabel.ifBlank {
                PhoneNormalizer.getCategoryDisplayLabel(dto.primaryCategory)
            },
            topCommunityLabel = label,
            tagsSummary = dto.tags.joinToString(", ") { "${it.tag} (${it.count})" },
            uniqueReporters = dto.uniqueReporters,
            syncVersion = 0L,
            cachedAtEpochMs = nowEpochMs,
            availability = LookupAvailability.AVAILABLE.name,
            source = "SERVER",
            fetchedAtEpochMs = nowEpochMs,
            expiresAtEpochMs = nowEpochMs + PhoneNormalizer.CACHE_DISPLAY_TTL_MS,
            serverVersion = 0L,
            // Metadata verifikasi hanya berlaku pada respons langsung; backend belum mengirim expiry.
            verifiedLabel = null
        )
    }

    private fun mapEmergencyDtoToEntity(dto: EmergencyDirectoryDto): EmergencyContact {
        val phones = dto.contacts.phoneNumbers.orEmpty()
        val primary = phones.firstOrNull() ?: dto.contacts.shortCode.orEmpty()
        val secondary = phones.drop(1).firstOrNull().orEmpty()
        val coordinates = dto.location?.coordinates?.takeIf {
            dto.location.type == "Point" && it.size == 2 && it[0] in -180.0..180.0 && it[1] in -90.0..90.0
        }
        return EmergencyContact(
            code = dto.code,
            scope = dto.scope,
            agencyType = dto.agencyType,
            name = dto.name,
            province = dto.province.orEmpty(),
            cityRegency = dto.cityRegency.orEmpty(),
            shortCode = dto.contacts.shortCode.orEmpty(),
            primaryPhone = primary,
            secondaryPhone = secondary,
            whatsappNumber = dto.contacts.whatsappNumber.orEmpty(),
            whatsappTemplate = dto.contacts.whatsappTemplate.orEmpty(),
            capabilitiesSummary = dto.capabilities.orEmpty().joinToString(" • "),
            address = dto.address.orEmpty(),
            is24Hours = dto.is24Hours,
            sourceUrl = dto.sourceUrl.orEmpty(),
            verifiedAt = dto.verifiedAt.orEmpty(),
            serverVersion = dto.syncVersion,
            latitude = coordinates?.get(1),
            longitude = coordinates?.get(0)
        )
    }

    companion object {
        /**
         * Resolusi status pencarian 2-dimensi (Availability x Risk/Confidence) secara deterministik (T-303).
         * Fungsi murni tanpa ketergantungan Android Context agar mudah diuji di JVM unit test.
         */
        fun resolveLookupResult(
            e164: String,
            availability: LookupAvailability,
            entity: CachedPhoneTag?,
            nowEpochMs: Long = System.currentTimeMillis(),
            categoryBreakdown: Map<String, Int> = emptyMap(),
            recentReports: List<SpamReportItemDto> = emptyList(),
            lookupLatencyMs: Double = 0.0
        ): FullPhoneLookupResult {
            if (availability == LookupAvailability.NOT_FOUND || (availability == LookupAvailability.AVAILABLE && entity == null)) {
                val unknownEntity = CachedPhoneTag(
                    phoneE164 = e164,
                    carrier = PhoneNormalizer.detectCarrierContext(e164),
                    reputationLevel = "UNKNOWN",
                    spamScore = 0.0,
                    isDanger = false,
                    primaryCategory = "",
                    primaryCategoryLabel = "Belum Ada Data",
                    topCommunityLabel = "Belum ada data tentang nomor ini",
                    tagsSummary = "",
                    uniqueReporters = 0,
                    syncVersion = 0L,
                    cachedAtEpochMs = nowEpochMs,
                    availability = LookupAvailability.NOT_FOUND.name,
                    source = "SERVER",
                    fetchedAtEpochMs = nowEpochMs,
                    expiresAtEpochMs = nowEpochMs
                )
                return FullPhoneLookupResult(
                    phoneTag = unknownEntity,
                    categoryBreakdown = categoryBreakdown,
                    recentReports = recentReports,
                    source = "SERVER",
                    lookupLatencyMs = lookupLatencyMs,
                    availability = LookupAvailability.NOT_FOUND,
                    displayStatus = LookupDisplayStatus.UNKNOWN,
                    confidence = ConfidenceTier.NONE,
                    isStale = false,
                    statusHeadline = "Belum ada data tentang nomor ini",
                    statusSubtext = "Nomor ini belum pernah dilaporkan komunitas CallTag. Ketidakadaan laporan bukan jaminan aman.",
                    verifiedLabel = null,
                    fetchedAtEpochMs = nowEpochMs
                )
            }

            if (entity == null) {
                val offlineEntity = CachedPhoneTag(
                    phoneE164 = e164,
                    carrier = PhoneNormalizer.detectCarrierContext(e164),
                    reputationLevel = "UNKNOWN",
                    spamScore = 0.0,
                    isDanger = false,
                    primaryCategory = "",
                    primaryCategoryLabel = "Offline",
                    topCommunityLabel = "Tidak dapat mengecek nomor saat ini (offline)",
                    tagsSummary = "",
                    uniqueReporters = 0,
                    syncVersion = 0L,
                    cachedAtEpochMs = nowEpochMs,
                    availability = LookupAvailability.UNAVAILABLE.name,
                    source = "OFFLINE",
                    fetchedAtEpochMs = 0L,
                    expiresAtEpochMs = 0L
                )
                return FullPhoneLookupResult(
                    phoneTag = offlineEntity,
                    categoryBreakdown = categoryBreakdown,
                    recentReports = recentReports,
                    source = "OFFLINE",
                    lookupLatencyMs = lookupLatencyMs,
                    availability = LookupAvailability.UNAVAILABLE,
                    displayStatus = LookupDisplayStatus.OFFLINE,
                    confidence = ConfidenceTier.NONE,
                    isStale = false,
                    statusHeadline = "Tidak dapat mengecek nomor saat ini (offline)",
                    statusSubtext = "Koneksi server tidak tersedia dan nomor belum ada di cache lokal. Tetap waspada terhadap panggilan tak dikenal.",
                    verifiedLabel = null,
                    fetchedAtEpochMs = 0L
                )
            }

            val ageMs = if (entity.fetchedAtEpochMs > 0L) (nowEpochMs - entity.fetchedAtEpochMs).coerceAtLeast(0L) else 0L
            val staleByAge = entity.fetchedAtEpochMs > 0L && ageMs > PhoneNormalizer.CACHE_DISPLAY_TTL_MS
            val staleByExpiry = entity.expiresAtEpochMs in 1L..nowEpochMs
            val isStale = !entity.source.equals("FIXTURE", ignoreCase = true) && (staleByAge || staleByExpiry)
            val ageDays = ageMs / (24L * 60 * 60 * 1000L)

            val hasCurrentVerification = availability == LookupAvailability.AVAILABLE && !isStale &&
                entity.source.equals("SERVER", ignoreCase = true) && !entity.verifiedLabel.isNullOrBlank()
            val confidence = when {
                hasCurrentVerification -> ConfidenceTier.HIGH
                entity.uniqueReporters >= 5 -> ConfidenceTier.HIGH
                entity.uniqueReporters >= 2 -> ConfidenceTier.MEDIUM
                entity.uniqueReporters == 1 -> ConfidenceTier.LOW
                else -> ConfidenceTier.NONE
            }

            val displayStatus = when {
                hasCurrentVerification -> LookupDisplayStatus.VERIFIED
                entity.reputationLevel.equals(ReputationLevels.DANGER_SCAM, ignoreCase = true) -> LookupDisplayStatus.DANGER_SCAM
                entity.reputationLevel.equals(ReputationLevels.SPAM, ignoreCase = true) -> LookupDisplayStatus.SPAM
                entity.reputationLevel.equals(ReputationLevels.SUSPICIOUS, ignoreCase = true) -> LookupDisplayStatus.SUSPICIOUS
                entity.reputationLevel.equals(ReputationLevels.CLEAN, ignoreCase = true) && entity.uniqueReporters > 0 -> LookupDisplayStatus.CLEAN_OBSERVED
                else -> LookupDisplayStatus.UNKNOWN
            }

            val headline = when (displayStatus) {
                LookupDisplayStatus.VERIFIED -> "Terverifikasi Resmi: ${entity.verifiedLabel}"
                LookupDisplayStatus.DANGER_SCAM -> "Terdeteksi Berbahaya (${entity.uniqueReporters} pelapor)"
                LookupDisplayStatus.SPAM -> "Dilaporkan Sebagai Spam (${entity.uniqueReporters} pelapor)"
                LookupDisplayStatus.SUSPICIOUS -> "Perlu Waspada (${entity.uniqueReporters} pelapor)"
                LookupDisplayStatus.CLEAN_OBSERVED -> "Pernah Dicek (Laporan Historis Rendah)"
                LookupDisplayStatus.UNKNOWN -> "Belum ada data tentang nomor ini"
                LookupDisplayStatus.OFFLINE -> "Tidak dapat mengecek nomor saat ini (offline)"
            }

            val lowConfidenceNote = if (confidence == ConfidenceTier.LOW) {
                " Baru dilaporkan 1 pengguna (keyakinan rendah), belum cukup bukti kuat."
            } else {
                ""
            }
            val staleNote = if (isStale) {
                " Data cache offline berusia $ageDays hari dan mungkin kedaluwarsa."
            } else {
                ""
            }
            val sourceNote = when {
                entity.source.equals("FIXTURE", ignoreCase = true) -> " [Data contoh lokal DEBUG]."
                availability == LookupAvailability.UNAVAILABLE -> " [Sumber: cache offline]."
                else -> ""
            }

            val baseSubtext = when (displayStatus) {
                LookupDisplayStatus.VERIFIED -> "Nomor tercatat pada direktori resmi terverifikasi."
                LookupDisplayStatus.DANGER_SCAM -> "Skor risiko ${entity.spamScore.toInt()}/100. Jangan pernah bagikan OTP, PIN, atau pasang file APK."
                LookupDisplayStatus.SPAM -> "Skor risiko ${entity.spamScore.toInt()}/100. Panggilan promosi/spam berulang dilaporkan komunitas."
                LookupDisplayStatus.SUSPICIOUS -> "Ada indikasi mencurigakan dari komunitas.${lowConfidenceNote}"
                LookupDisplayStatus.CLEAN_OBSERVED -> "Skor saat ini ${entity.spamScore.toInt()}/100 dari ${entity.uniqueReporters} pelapor historis.${lowConfidenceNote}"
                LookupDisplayStatus.UNKNOWN -> "Belum ada laporan komunitas. Ketidakadaan laporan bukan jaminan aman."
                LookupDisplayStatus.OFFLINE -> "Perangkat sedang offline."
            }

            val effectiveSource = when {
                entity.source.equals("FIXTURE", ignoreCase = true) -> "FIXTURE"
                availability == LookupAvailability.UNAVAILABLE -> "CACHE"
                else -> entity.source.ifBlank { "SERVER" }
            }

            return FullPhoneLookupResult(
                phoneTag = entity.copy(
                    carrier = entity.carrier.ifBlank { PhoneNormalizer.detectCarrierContext(e164) },
                    verifiedLabel = entity.verifiedLabel.takeIf { hasCurrentVerification }
                ),
                categoryBreakdown = categoryBreakdown,
                recentReports = recentReports,
                source = effectiveSource,
                lookupLatencyMs = lookupLatencyMs,
                availability = availability,
                displayStatus = displayStatus,
                confidence = confidence,
                isStale = isStale,
                statusHeadline = headline,
                statusSubtext = (baseSubtext + staleNote + sourceNote).trim(),
                verifiedLabel = entity.verifiedLabel.takeIf { hasCurrentVerification },
                fetchedAtEpochMs = entity.fetchedAtEpochMs
            )
        }
    }
}
